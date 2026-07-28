package com.vetsoftware.app.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistration;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * Lleva al manejo global las excepciones que ocurren antes del {@code DispatcherServlet}.
 *
 * <p>El filtro corre inmediatamente después de {@link ServerHttpObservationFilter}; por ello el
 * span HTTP y sus campos MDC siguen activos cuando {@link GlobalExceptionHandler} escribe el log y
 * construye el {@code ProblemDetail}. Las excepciones ya manejadas por Spring MVC nunca llegan a
 * este puente, lo que evita logs y respuestas duplicados.
 */
@Component
@FilterRegistration(
        order = Ordered.HIGHEST_PRECEDENCE + 2,
        dispatcherTypes = DispatcherType.REQUEST,
        asyncSupported = true)
public final class CorrelatedExceptionBridgeFilter extends OncePerRequestFilter {

    private final HandlerExceptionResolver exceptionResolver;

    public CorrelatedExceptionBridgeFilter(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            ServerHttpObservationFilter.findObservationContext(request)
                    .ifPresent(context -> context.setError(exception));

            ModelAndView resolution =
                    exceptionResolver.resolveException(request, response, null, exception);
            if (resolution == null) {
                throw exception;
            }
        }
    }
}
