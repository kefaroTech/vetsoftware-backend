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
        try (Scope ignored = Context.root().makeCurrent()) {
            chain.doFilter(request, response);
        }
    }
}
