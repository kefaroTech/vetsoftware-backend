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

        // Contexto HTTP + IP de origen para TODA request (incluidas las públicas: login, etc.), de modo
        // que cada log de la request quede autocontenido (sabes la ruta que falló sin ir a la traza) y
        // cada evento AUDIT (login_failure, unauthenticated, access_denied, http_mutation) lo lleve.
        // getRemoteAddr() es proxy-aware y NO falsificable porque server.forward-headers-strategy=native
        // hace que Tomcat solo confíe en X-Forwarded-For de proxies internos de confianza.
        MDC.put(MdcKeys.HTTP_METHOD, request.getMethod());
        MDC.put(MdcKeys.HTTP_PATH, request.getRequestURI());
        MDC.put(MdcKeys.CLIENT_IP, request.getRemoteAddr());
        try (Scope ignored = Context.root().makeCurrent()) {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MdcKeys.HTTP_METHOD);
            MDC.remove(MdcKeys.HTTP_PATH);
            MDC.remove(MdcKeys.CLIENT_IP);
        }
    }
}
