package com.vetsoftware.app.infrastructure.web;

import com.vetsoftware.app.infrastructure.logging.MdcKeys;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceContextResetFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        MDC.remove("traceId");
        MDC.remove("spanId");
        // Limpieza defensiva del actor: este filtro es el más externo y siempre corre, así que
        // garantiza que ningún hilo reutilizado arrastre la identidad de una request anterior
        // (incluso en rutas públicas donde AuthFilter no se ejecuta).
        MDC.remove(MdcKeys.ACTOR_TYPE);
        MDC.remove(MdcKeys.ACTOR_EMPLOYEE_ID);
        MDC.remove(MdcKeys.ACTOR_COMPANY_ID);
        MDC.remove(MdcKeys.ACTOR_SYSTEM_USER_ID);

        // IP de origen para TODA request (incluidas las públicas: login, etc.), de modo que cada
        // evento AUDIT (login_failure, unauthenticated, access_denied, http_mutation) la lleve vía MDC.
        MDC.put(MdcKeys.CLIENT_IP, clientIp(request));
        try (Scope ignored = Context.root().makeCurrent()) {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MdcKeys.CLIENT_IP);
        }
    }

    /**
     * IP del cliente. Prioriza {@code X-Forwarded-For} (primer salto) y {@code X-Real-IP} para el caso
     * detrás de proxy/balanceador; si no, {@code getRemoteAddr()}.
     *
     * <p>Cuidado: {@code X-Forwarded-For} es falsificable si NO hay un proxy de confianza que lo
     * reescriba. Si despliegas tras un proxy conocido, lo robusto es {@code server.forward-headers-strategy}
     * y usar solo {@code getRemoteAddr()}.
     */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
