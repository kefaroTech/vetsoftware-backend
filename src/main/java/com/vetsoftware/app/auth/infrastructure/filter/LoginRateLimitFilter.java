package com.vetsoftware.app.auth.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limiting distribuido por IP para rutas publicas de autenticacion.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final RouteLimit LOGIN_LIMIT = new RouteLimit(
            "login-rl:",
            "/auth/login",
            5,
            Duration.ofMinutes(1),
            "LOGIN_RATE_LIMITED",
            "Too many login attempts. Try again later.");
    private static final RouteLimit REGISTER_LIMIT = new RouteLimit(
            "register-rl:",
            "/register",
            3,
            Duration.ofHours(1),
            "REGISTER_RATE_LIMITED",
            "Too many registration attempts. Try again later.");
    private static final RouteLimit REFRESH_LIMIT = new RouteLimit(
            "refresh-rl:",
            "/auth/refresh",
            30,
            Duration.ofMinutes(1),
            "REFRESH_RATE_LIMITED",
            "Too many token refresh attempts. Try again later.");

    private final LettuceBasedProxyManager<String> proxyManager;
    private final ObjectMapper objectMapper;
    private final AuditLogger auditLogger;

    public LoginRateLimitFilter(LettuceBasedProxyManager<String> loginRateLimitProxyManager,
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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        RouteLimit routeLimit = routeLimit(request);
        if (routeLimit == null) {
            chain.doFilter(request, response);
            return;
        }

        BucketProxy bucket = proxyManager.builder().build(
                clientKey(request, routeLimit),
                () -> bucketConfiguration(routeLimit));
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }

        auditLogger.rateLimited(routeLimit.code());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(routeLimit.window().toSeconds()));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "type", "about:blank",
                "title", "Too Many Requests",
                "status", 429,
                "code", routeLimit.code(),
                "detail", routeLimit.detail()
        ));
    }

    private static RouteLimit routeLimit(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return null;
        String uri = request.getServletPath();
        if (uri.equals(REFRESH_LIMIT.path())) return REFRESH_LIMIT;
        if (uri.contains(LOGIN_LIMIT.path())) return LOGIN_LIMIT;
        if (uri.equals(REGISTER_LIMIT.path())) return REGISTER_LIMIT;
        return null;
    }

    private static BucketConfiguration bucketConfiguration(RouteLimit routeLimit) {
        return BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(routeLimit.maxAttempts())
                        .refillIntervally(routeLimit.maxAttempts(), routeLimit.window()))
                .build();
    }

    private static String clientKey(HttpServletRequest request, RouteLimit routeLimit) {
        return routeLimit.keyPrefix() + request.getRemoteAddr();
    }

    private record RouteLimit(String keyPrefix, String path, int maxAttempts, Duration window,
                              String code, String detail) {}
}
