package com.vetsoftware.app.auth.infrastructure.security;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Traduce los rechazos que ocurren dentro de la cadena de filtros —no en un
 * controller— al mismo {@code ProblemDetail} (RFC 7807) que emiten
 * {@code AuthFilter} y {@code GlobalExceptionHandler}.
 *
 * <p>
 * Sin esto, el {@code anyRequest().authenticated()} de {@code SecurityConfig}
 * responde con el 403 vacío por defecto de Spring Security: el front no
 * encontraría el {@code code} que usa para decidir entre refrescar el token y
 * desloguear, y el rechazo no quedaría en la auditoría.
 */
@Component
public class SecurityProblemDetailHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final AuditLogger auditLogger;
    private final Tracer tracer;

    public SecurityProblemDetailHandler(ObjectMapper objectMapper, AuditLogger auditLogger,
            Tracer tracer) {
        this.objectMapper = objectMapper;
        this.auditLogger = auditLogger;
        this.tracer = tracer;
    }

    /** Request sin principal sobre una ruta que exige autenticación. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authenticationException) throws IOException {
        String detail = "Authentication required";
        auditLogger.unauthenticated(request.getMethod(), request.getRequestURI(), detail);
        write(response, HttpStatus.UNAUTHORIZED, "TOKEN_MISSING", detail);
    }

    /** Request autenticada pero sin autorización sobre el recurso. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        auditLogger.accessDenied(request.getMethod(), request.getRequestURI());
        write(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied");
    }

    private void write(HttpServletResponse response, HttpStatus status, String code, String detail)
            throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            problem.setProperty("traceId", currentSpan.context().traceId());
        }
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
