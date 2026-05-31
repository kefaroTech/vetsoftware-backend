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
 * Rate limiting de login por IP. Los buckets viven en Redis (ver {@code RateLimitConfig}): se
 * comparten entre réplicas y expiran por TTL, así que no hay fuga de memoria ni se puede evadir
 * el límite repartiendo intentos entre instancias.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final LettuceBasedProxyManager<String> proxyManager;
    private final BucketConfiguration bucketConfiguration;
    private final ObjectMapper objectMapper;
    private final AuditLogger auditLogger;

    public LoginRateLimitFilter(LettuceBasedProxyManager<String> loginRateLimitProxyManager,
                                ObjectMapper objectMapper,
                                AuditLogger auditLogger) {
        this.proxyManager = loginRateLimitProxyManager;
        this.objectMapper = objectMapper;
        this.auditLogger = auditLogger;
        this.bucketConfiguration = BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(MAX_ATTEMPTS).refillIntervally(MAX_ATTEMPTS, WINDOW))
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().contains("/auth/login");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        BucketProxy bucket = proxyManager.builder().build(clientKey(request), () -> bucketConfiguration);
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            auditLogger.loginRateLimited();
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "type", "about:blank",
                    "title", "Too Many Requests",
                    "status", 429,
                    "code", "LOGIN_RATE_LIMITED",
                    "detail", "Too many login attempts. Try again later."
            ));
        }
    }

    private static String clientKey(HttpServletRequest request) {
        // IP real y NO falsificable (server.forward-headers-strategy=native). Prefijo de namespace
        // para no colisionar con otras claves en el mismo Redis.
        return "login-rl:" + request.getRemoteAddr();
    }
}
