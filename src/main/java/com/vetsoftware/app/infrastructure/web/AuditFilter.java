package com.vetsoftware.app.infrastructure.web;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Emite un evento de auditoría por cada mutación en el borde HTTP
 * (POST/PUT/PATCH/DELETE).
 *
 * <p>
 * Sin {@code @Order} → se registra como el filtro servlet más interno, por lo
 * que corre dentro de la cadena de seguridad y el MDC con el actor ya está
 * poblado por {@code AuthFilter} cuando emite el evento.
 */
@Component
public class AuditFilter extends OncePerRequestFilter {

    private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final AuditLogger auditLogger;

    public AuditFilter(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            if (MUTATING.contains(request.getMethod().toUpperCase())) {
                int status = response.getStatus();
                auditLogger.mutation(request.getMethod(), request.getRequestURI(), status,
                        outcome(status), System.currentTimeMillis() - start);
            }
        }
    }

    private static String outcome(int status) {
        if (status >= 500)
            return "ERROR";
        if (status == 401 || status == 403)
            return "DENIED";
        if (status >= 400)
            return "REJECTED";
        return "SUCCESS";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator") || uri.startsWith("/swagger")
                || uri.startsWith("/v3/api-docs");
    }
}
