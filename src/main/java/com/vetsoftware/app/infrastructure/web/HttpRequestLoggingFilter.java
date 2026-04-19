package com.vetsoftware.app.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpRequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            logRequest(request, response.getStatus(), System.currentTimeMillis() - start);
        }
    }

    private void logRequest(HttpServletRequest request, int status, long ms) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        if (status >= 500)      log.error("HTTP {} {} → {} ({}ms)", method, uri, status, ms);
        else if (status >= 400) log.warn ("HTTP {} {} → {} ({}ms)", method, uri, status, ms);
        else                    log.info ("HTTP {} {} → {} ({}ms)", method, uri, status, ms);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator") || uri.startsWith("/swagger") || uri.startsWith("/v3/api-docs");
    }
}
