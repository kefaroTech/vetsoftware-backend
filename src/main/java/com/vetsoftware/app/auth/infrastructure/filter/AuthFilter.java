package com.vetsoftware.app.auth.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsoftware.app.auth.application.annotation.PublicEndpoint;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.auth.application.port.in.ResolveAuthContextUseCase;
import com.vetsoftware.app.auth.application.port.in.ResolveSystemAuthContextUseCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Component
public class AuthFilter extends OncePerRequestFilter {

    private final ResolveAuthContextUseCase resolveAuthContextUseCase;
    private final ResolveSystemAuthContextUseCase resolveSystemAuthContextUseCase;
    private final ObjectMapper objectMapper;
    private final RequestMappingHandlerMapping handlerMapping;

    public AuthFilter(ResolveAuthContextUseCase resolveAuthContextUseCase,
                      ResolveSystemAuthContextUseCase resolveSystemAuthContextUseCase,
                      ObjectMapper objectMapper,
                      @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.resolveAuthContextUseCase = resolveAuthContextUseCase;
        this.resolveSystemAuthContextUseCase = resolveSystemAuthContextUseCase;
        this.objectMapper = objectMapper;
        this.handlerMapping = handlerMapping;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        try {
            Object handler = handlerMapping.getHandler(request).getHandler();
            return handler instanceof HandlerMethod method
                    && method.hasMethodAnnotation(PublicEndpoint.class);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            writeUnauthorized(response, "Missing or invalid Authorization header");
            return;
        }

        String token = header.substring(7);
        String type = extractType(token);
        Long id = extractId(token);

        if (type == null || id == null) {
            writeUnauthorized(response, "Invalid token");
            return;
        }

        AuthContext authContext = switch (type) {
            case "EMPLOYEE"    -> resolveAuthContextUseCase.execute(id);
            case "SYSTEM_USER" -> resolveSystemAuthContextUseCase.execute(id);
            default            -> null;
        };

        if (authContext == null) {
            writeUnauthorized(response, "Unknown user type");
            return;
        }

        request.setAttribute("authContext", authContext);
        filterChain.doFilter(request, response);
    }

    private String extractType(String token) {
        // TODO: reemplazar con extracción desde JWT cuando se implemente
        return null;
    }

    private Long extractId(String token) {
        // TODO: reemplazar con extracción desde JWT cuando se implemente
        try {
            return Long.parseLong(token);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("error", message));
    }
}
