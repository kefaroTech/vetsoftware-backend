package com.vetsoftware.app.auth.infrastructure.filter;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Rate limiting distribuido por IP y credencial para rutas publicas sensibles. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LoginRateLimitFilter extends OncePerRequestFilter {

  private static final int MAX_ACCOUNT_BODY_BYTES = 16 * 1024;

  private static final RouteLimit LOGIN_LIMIT =
      new RouteLimit(
          "login-rl:",
          "/auth/login",
          5,
          Duration.ofMinutes(1),
          "LOGIN_RATE_LIMITED",
          "Too many login attempts. Try again later.",
          List.of("employeeCode", "code"));
  private static final RouteLimit REGISTER_LIMIT =
      new RouteLimit(
          "register-rl:",
          "/register",
          3,
          Duration.ofHours(1),
          "REGISTER_RATE_LIMITED",
          "Too many registration attempts. Try again later.",
          List.of("employeeEmail", "companyIdentifier"));
  private static final RouteLimit REFRESH_LIMIT =
      new RouteLimit(
          "refresh-rl:",
          "/auth/refresh",
          30,
          Duration.ofMinutes(1),
          "REFRESH_RATE_LIMITED",
          "Too many token refresh attempts. Try again later.",
          List.of("refreshToken"));
  private static final RouteLimit FORGOT_PASSWORD_LIMIT =
      new RouteLimit(
          "forgot-password-rl:",
          "/auth/forgot-password",
          3,
          Duration.ofHours(1),
          "FORGOT_PASSWORD_RATE_LIMITED",
          "Too many password reset attempts. Try again later.",
          List.of("employeeCode"));
  private static final RouteLimit DIAN_WEBHOOK_LIMIT =
      new RouteLimit(
          "dian-webhook-rl:",
          "/dian/webhooks",
          120,
          Duration.ofMinutes(1),
          "DIAN_WEBHOOK_RATE_LIMITED",
          "Too many webhook requests. Try again later.",
          List.of());

  private final LettuceBasedProxyManager<String> proxyManager;
  private final ObjectMapper objectMapper;
  private final AuditLogger auditLogger;

  public LoginRateLimitFilter(
      LettuceBasedProxyManager<String> loginRateLimitProxyManager,
      ObjectMapper objectMapper,
      AuditLogger auditLogger) {
    this.proxyManager = loginRateLimitProxyManager;
    this.objectMapper = objectMapper;
    this.auditLogger = auditLogger;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return routeLimit(request) == null;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    RouteLimit routeLimit = routeLimit(request);
    if (routeLimit == null) {
      chain.doFilter(request, response);
      return;
    }

    if (!tryConsume(routeLimit, ipKey(request, routeLimit))) {
      writeRateLimited(response, routeLimit);
      return;
    }

    HttpServletRequest requestForChain = request;
    for (String accountKey : pathAccountKeys(request, routeLimit)) {
      if (!tryConsume(routeLimit, accountKey)) {
        writeRateLimited(response, routeLimit);
        return;
      }
    }

    if (!routeLimit.accountFields().isEmpty()) {
      byte[] body = request.getInputStream().readNBytes(MAX_ACCOUNT_BODY_BYTES + 1);
      if (body.length > MAX_ACCOUNT_BODY_BYTES) {
        writeProblem(
            response,
            HttpStatus.PAYLOAD_TOO_LARGE,
            "REQUEST_BODY_TOO_LARGE",
            "Request body is too large for this endpoint.");
        return;
      }

      requestForChain = new CachedBodyRequest(request, body);
      for (String accountKey : bodyAccountKeys(body, routeLimit)) {
        if (!tryConsume(routeLimit, accountKey)) {
          writeRateLimited(response, routeLimit);
          return;
        }
      }
    }

    chain.doFilter(requestForChain, response);
  }

  private static RouteLimit routeLimit(HttpServletRequest request) {
    if (!"POST".equalsIgnoreCase(request.getMethod())) return null;
    String uri = request.getServletPath();
    if (uri.equals(REFRESH_LIMIT.path())) return REFRESH_LIMIT;
    if (uri.startsWith(LOGIN_LIMIT.path() + "/")) return LOGIN_LIMIT;
    if (uri.equals(REGISTER_LIMIT.path())) return REGISTER_LIMIT;
    if (uri.equals(FORGOT_PASSWORD_LIMIT.path())) return FORGOT_PASSWORD_LIMIT;
    if (uri.startsWith(DIAN_WEBHOOK_LIMIT.path() + "/")) return DIAN_WEBHOOK_LIMIT;
    return null;
  }

  private static BucketConfiguration bucketConfiguration(RouteLimit routeLimit) {
    return BucketConfiguration.builder()
        .addLimit(
            limit ->
                limit
                    .capacity(routeLimit.maxAttempts())
                    .refillIntervally(routeLimit.maxAttempts(), routeLimit.window()))
        .build();
  }

  private boolean tryConsume(RouteLimit routeLimit, String key) {
    BucketProxy bucket = proxyManager.builder().build(key, () -> bucketConfiguration(routeLimit));
    return bucket.tryConsume(1);
  }

  private static String ipKey(HttpServletRequest request, RouteLimit routeLimit) {
    return routeLimit.keyPrefix() + "ip:" + request.getRemoteAddr();
  }

  private static List<String> pathAccountKeys(HttpServletRequest request, RouteLimit routeLimit) {
    if (routeLimit != DIAN_WEBHOOK_LIMIT) return List.of();
    String provider = request.getServletPath().substring(routeLimit.path().length() + 1).trim();
    if (provider.isEmpty() || provider.contains("/")) return List.of();
    return List.of(accountKey(routeLimit, "provider", provider));
  }

  private List<String> bodyAccountKeys(byte[] body, RouteLimit routeLimit) {
    if (body.length == 0) return List.of();
    try {
      JsonNode root = objectMapper.readTree(body);
      List<String> keys = new ArrayList<>(routeLimit.accountFields().size());
      for (String field : routeLimit.accountFields()) {
        JsonNode valueNode = root.get(field);
        if (valueNode == null || !valueNode.isString()) continue;
        String value = valueNode.asText().trim();
        if (value.isEmpty()) continue;
        if (!"refreshToken".equals(field)) value = value.toLowerCase(Locale.ROOT);
        keys.add(accountKey(routeLimit, field, value));
      }
      return keys;
    } catch (RuntimeException ignored) {
      // El controller conserva la responsabilidad de reportar JSON invalido; el limite por IP ya se
      // consumio.
      return List.of();
    }
  }

  private static String accountKey(RouteLimit routeLimit, String field, String value) {
    return routeLimit.keyPrefix() + "account:" + sha256(field + ':' + value);
  }

  private static String sha256(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private void writeRateLimited(HttpServletResponse response, RouteLimit routeLimit)
      throws IOException {
    auditLogger.rateLimited(routeLimit.code());
    response.setHeader("Retry-After", String.valueOf(routeLimit.window().toSeconds()));
    writeProblem(response, HttpStatus.TOO_MANY_REQUESTS, routeLimit.code(), routeLimit.detail());
  }

  private void writeProblem(
      HttpServletResponse response, HttpStatus status, String code, String detail)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(
        response.getWriter(),
        Map.of(
            "type",
            "about:blank",
            "title",
            status.getReasonPhrase(),
            "status",
            status.value(),
            "code",
            code,
            "detail",
            detail));
  }

  private record RouteLimit(
      String keyPrefix,
      String path,
      int maxAttempts,
      Duration window,
      String code,
      String detail,
      List<String> accountFields) {}

  private static final class CachedBodyRequest extends HttpServletRequestWrapper {
    private final byte[] body;

    private CachedBodyRequest(HttpServletRequest request, byte[] body) {
      super(request);
      this.body = body;
    }

    @Override
    public ServletInputStream getInputStream() {
      ByteArrayInputStream input = new ByteArrayInputStream(body);
      return new ServletInputStream() {
        @Override
        public boolean isFinished() {
          return input.available() == 0;
        }

        @Override
        public boolean isReady() {
          return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
          // La copia en memoria siempre esta disponible de forma sincrona.
        }

        @Override
        public int read() {
          return input.read();
        }

        @Override
        public int read(byte[] bytes, int offset, int length) {
          return input.read(bytes, offset, length);
        }
      };
    }

    @Override
    public BufferedReader getReader() {
      String encoding = getCharacterEncoding();
      Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
      return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }

    @Override
    public int getContentLength() {
      return body.length;
    }

    @Override
    public long getContentLengthLong() {
      return body.length;
    }
  }
}
