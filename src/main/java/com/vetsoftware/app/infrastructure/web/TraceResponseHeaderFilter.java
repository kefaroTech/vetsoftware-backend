package com.vetsoftware.app.infrastructure.web;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public final class TraceResponseHeaderFilter extends OncePerRequestFilter {

    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String LEGACY_REQUEST_HEADER = "X-Request-Id";

    private final Tracer tracer;

    public TraceResponseHeaderFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Span span = tracer.currentSpan();
        if (span != null) {
            String traceId = span.context().traceId();
            response.setHeader(TRACE_HEADER, traceId);
            // Compatibilidad temporal con clientes existentes. X-Trace-Id es el contrato nuevo.
            response.setHeader(LEGACY_REQUEST_HEADER, traceId);
        }
        chain.doFilter(request, response);
    }
}
