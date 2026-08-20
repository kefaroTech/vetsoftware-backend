package com.vetsoftware.app.auth.infrastructure.security;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
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

    /**
     * Request sin principal sobre una ruta que exige autenticación.
     *
     * <p>
     * A la auditoría va el {@code code} en snake_case y no el {@code detail}: el
     * campo {@code reason} del canal AUDIT es vocabulario cerrado
     * —{@code token_missing}, {@code token_expired}, {@code token_invalid},
     * {@code session_replaced}—, mismo criterio que {@code AuthFilter}. Antes se
     * auditaba la prosa {@code "Authentication required"} mientras el front recibía
     * {@code TOKEN_MISSING}: dos nombres para el mismo hecho, y el filtro de
     * Grafana que agrupara por {@code reason} dejaba fuera este camino sin dar
     * ningún error.
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authenticationException) throws IOException {
        String code = "TOKEN_MISSING";
        auditLogger.unauthenticated(request.getMethod(), request.getRequestURI(),
                code.toLowerCase(Locale.ROOT));
        write(response, HttpStatus.UNAUTHORIZED, code, "Authentication required");
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
