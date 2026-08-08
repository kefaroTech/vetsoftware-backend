package com.vetsoftware.app.auth.infrastructure.filter;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.auth.application.dto.EmployeeContext;
import com.vetsoftware.app.auth.application.dto.SystemContext;
import com.vetsoftware.app.auth.application.dto.SystemUserContext;
import com.vetsoftware.app.auth.application.exception.SessionReplacedException;
import com.vetsoftware.app.auth.application.port.in.ResolveAuthContextUseCase;
import com.vetsoftware.app.auth.application.port.in.ResolveSystemAuthContextUseCase;
import com.vetsoftware.app.auth.infrastructure.config.PublicRoutes;
import com.vetsoftware.app.auth.infrastructure.security.JwtProvider;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.infrastructure.logging.MdcKeys;
import io.jsonwebtoken.ExpiredJwtException;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
public class AuthFilter extends OncePerRequestFilter {

    private static final String SYSTEM_ROLE = "ROLE_SYSTEM";

    private final ResolveAuthContextUseCase resolveAuthContextUseCase;
    private final ResolveSystemAuthContextUseCase resolveSystemAuthContextUseCase;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;
    private final AuditLogger auditLogger;
    private final Tracer tracer;

    public AuthFilter(ResolveAuthContextUseCase resolveAuthContextUseCase,
            ResolveSystemAuthContextUseCase resolveSystemAuthContextUseCase,
            JwtProvider jwtProvider, ObjectMapper objectMapper, AuditLogger auditLogger,
            Tracer tracer) {
        this.resolveAuthContextUseCase = resolveAuthContextUseCase;
        this.resolveSystemAuthContextUseCase = resolveSystemAuthContextUseCase;
        this.jwtProvider = jwtProvider;
        this.objectMapper = objectMapper;
        this.auditLogger = auditLogger;
        this.tracer = tracer;
    }

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * Las rutas viven en {@link PublicRoutes}, compartidas con
     * {@code SecurityConfig}: la cadena de filtros de Spring Security tiene que
     * permitir exactamente lo mismo que este filtro deja pasar, o una ruta pública
     * terminaría en 403.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()))
            return true;
        String method = request.getMethod();
        String path = request.getServletPath();
        return PublicRoutes.JWT_EXCLUDED.stream()
                .anyMatch(r -> (r.anyMethod() || r.method().matches(method))
                        && PATH_MATCHER.match(r.pattern(), path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            writeUnauthorized(request, response, "TOKEN_MISSING",
                    "Missing or invalid Authorization header");
            return;
        }

        String token = header.substring(7);
        String type;
        Long id;
        Long authVersion;
        try {
            type = jwtProvider.extractType(token);
            id = jwtProvider.extractId(token);
            authVersion = jwtProvider.extractAuthVersion(token);
        } catch (ExpiredJwtException e) {
            writeUnauthorized(request, response, "TOKEN_EXPIRED", "Token expired");
            return;
        } catch (Exception e) {
            writeUnauthorized(request, response, "TOKEN_INVALID", "Invalid token");
            return;
        }

        if (type == null || id == null) {
            writeUnauthorized(request, response, "TOKEN_INVALID", "Invalid token");
            return;
        }

        AuthContext authContext;
        try {
            authContext = switch (type) {
                case "EMPLOYEE" -> resolveAuthContextUseCase.execute(id, authVersion);
                case "SYSTEM_USER" -> resolveSystemAuthContextUseCase.execute(id, authVersion);
                default -> null;
            };
        } catch (SessionReplacedException e) {
            writeUnauthorized(request, response, "SESSION_REPLACED", e.getMessage());
            return;
        }

        if (authContext == null) {
            writeUnauthorized(request, response, "TOKEN_INVALID", "Unknown or revoked user");
            return;
        }

        try {
            putActorToMdc(authContext);
            SecurityContextHolder.getContext().setAuthentication(toAuthentication(authContext));
            filterChain.doFilter(request, response);
        } finally {
            clearActorMdc();
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Pone la identidad del actor en el MDC para que aparezca como campos en cada
     * log de la request.
     */
    private static void putActorToMdc(AuthContext authContext) {
        if (authContext instanceof EmployeeContext employee) {
            MDC.put(MdcKeys.ACTOR_TYPE, "EMPLOYEE");
            MDC.put(MdcKeys.ACTOR_EMPLOYEE_ID, String.valueOf(employee.employeeId()));
            MDC.put(MdcKeys.ACTOR_COMPANY_ID, String.valueOf(employee.companyId()));
        } else if (authContext instanceof SystemUserContext systemUser) {
            MDC.put(MdcKeys.ACTOR_TYPE, "SYSTEM_USER");
            MDC.put(MdcKeys.ACTOR_SYSTEM_USER_ID, String.valueOf(systemUser.systemUserId()));
        } else if (authContext instanceof SystemContext) {
            MDC.put(MdcKeys.ACTOR_TYPE, "SYSTEM");
        }
    }

    private static void clearActorMdc() {
        MDC.remove(MdcKeys.ACTOR_TYPE);
        MDC.remove(MdcKeys.ACTOR_EMPLOYEE_ID);
        MDC.remove(MdcKeys.ACTOR_COMPANY_ID);
        MDC.remove(MdcKeys.ACTOR_SYSTEM_USER_ID);
    }

    private static UsernamePasswordAuthenticationToken toAuthentication(AuthContext authContext) {
        List<GrantedAuthority> authorities = new ArrayList<>(authContext.permissions().stream()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new).toList());
        if (authContext instanceof SystemContext || authContext instanceof SystemUserContext) {
            authorities.add(new SimpleGrantedAuthority(SYSTEM_ROLE));
        }
        return new UsernamePasswordAuthenticationToken(authContext, null, authorities);
    }

    /**
     * Rechazo de autenticación en formato ProblemDetail (RFC 7807), consistente con
     * {@code
     * GlobalExceptionHandler}. El {@code code} discrimina el motivo para que el
     * front decida: {@code
     * TOKEN_EXPIRED} → intentar refrescar; {@code SESSION_REPLACED},
     * {@code TOKEN_INVALID} o {@code
     * TOKEN_MISSING} → desloguear.
     */
    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response,
            String code, String detail) throws IOException {
        auditLogger.unauthenticated(request.getMethod(), request.getRequestURI(), detail);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
        problem.setTitle(HttpStatus.UNAUTHORIZED.getReasonPhrase());
        problem.setProperty("code", code);
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            problem.setProperty("traceId", currentSpan.context().traceId());
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
