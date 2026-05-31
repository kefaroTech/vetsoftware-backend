package com.vetsoftware.app.auth.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final AuditLogger auditLogger;

    public LoginRateLimitFilter(ObjectMapper objectMapper, AuditLogger auditLogger) {
        this.objectMapper = objectMapper;
        this.auditLogger = auditLogger;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().contains("/auth/login");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Bucket bucket = buckets.computeIfAbsent(clientKey(request), k -> newBucket());
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

    private static Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(MAX_ATTEMPTS).refillIntervally(MAX_ATTEMPTS, WINDOW).build())
                .build();
    }

    private static String clientKey(HttpServletRequest request) {
        // IP real y NO falsificable: server.forward-headers-strategy=native hace que Tomcat solo
        // confíe en X-Forwarded-For de proxies internos. Parsear el header a mano permitiría evadir
        // el límite mandando un XFF distinto en cada intento (un bucket nuevo por valor falso).
        return request.getRemoteAddr();
    }
}
