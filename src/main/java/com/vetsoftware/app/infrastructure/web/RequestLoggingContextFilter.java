package com.vetsoftware.app.infrastructure.web;

import com.vetsoftware.app.infrastructure.logging.MdcKeys;
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
public final class RequestLoggingContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        clearApplicationContext();

        // Contexto HTTP + IP de origen + user-agent para TODA request (incluidas las
        // públicas: login,
        // etc.), de modo que cada log de la request quede autocontenido (sabes la ruta
        // que falló sin ir
        // a la traza) y cada evento AUDIT (login_failure, login_rate_limited,
        // unauthenticated,
        // access_denied, http_mutation) los lleve. getRemoteAddr() es proxy-aware y NO
        // falsificable
        // porque server.forward-headers-strategy=native hace que Tomcat solo confíe en
        // X-Forwarded-For
        // de proxies internos de confianza. El User-Agent es opcional → solo se puebla
        // si viene.
        MDC.put(MdcKeys.HTTP_METHOD, request.getMethod());
        MDC.put(MdcKeys.HTTP_PATH, request.getRequestURI());
        MDC.put(MdcKeys.CLIENT_IP, request.getRemoteAddr());
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            MDC.put(MdcKeys.USER_AGENT, userAgent);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            clearApplicationContext();
        }
    }

    /**
     * Limpia exclusivamente el contexto propiedad de VetSoftware. El ciclo de vida
     * de traceId/spanId pertenece a Micrometer Tracing y nunca se manipula desde la
     * aplicación.
     */
    private static void clearApplicationContext() {
        MDC.remove(MdcKeys.ACTOR_TYPE);
        MDC.remove(MdcKeys.ACTOR_EMPLOYEE_ID);
        MDC.remove(MdcKeys.ACTOR_COMPANY_ID);
        MDC.remove(MdcKeys.ACTOR_SYSTEM_USER_ID);
        MDC.remove(MdcKeys.HTTP_METHOD);
        MDC.remove(MdcKeys.HTTP_PATH);
        MDC.remove(MdcKeys.CLIENT_IP);
        MDC.remove(MdcKeys.USER_AGENT);
    }
}
