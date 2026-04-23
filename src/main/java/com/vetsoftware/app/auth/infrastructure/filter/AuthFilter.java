package com.vetsoftware.app.auth.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.auth.application.port.in.ResolveAuthContextUseCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthFilter extends OncePerRequestFilter {

    private final ResolveAuthContextUseCase resolveAuthContextUseCase;
    private final ObjectMapper objectMapper;

    public AuthFilter(ResolveAuthContextUseCase resolveAuthContextUseCase, ObjectMapper objectMapper) {
        this.resolveAuthContextUseCase = resolveAuthContextUseCase;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            writeUnauthorized(response, "Missing or invalid Authorization header");
            return;
        }

        Long employeeId = extractEmployeeId(header.substring(7));
        if (employeeId == null) {
            writeUnauthorized(response, "Invalid token");
            return;
        }

        AuthContext authContext = resolveAuthContextUseCase.execute(employeeId);
        request.setAttribute("authContext", authContext);
        filterChain.doFilter(request, response);
    }

    private Long extractEmployeeId(String token) {
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
