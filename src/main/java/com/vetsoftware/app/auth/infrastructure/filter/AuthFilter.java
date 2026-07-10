package com.vetsoftware.app.auth.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.auth.application.dto.EmployeeContext;
import com.vetsoftware.app.auth.application.dto.SystemContext;
import com.vetsoftware.app.auth.application.dto.SystemUserContext;
import com.vetsoftware.app.auth.application.port.in.ResolveAuthContextUseCase;
import com.vetsoftware.app.auth.application.port.in.ResolveSystemAuthContextUseCase;
import com.vetsoftware.app.auth.infrastructure.security.JwtProvider;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.infrastructure.logging.MdcKeys;
import io.jsonwebtoken.ExpiredJwtException;
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

@Component
public class AuthFilter extends OncePerRequestFilter {

    private static final String SYSTEM_ROLE = "ROLE_SYSTEM";

    private final ResolveAuthContextUseCase resolveAuthContextUseCase;
    private final ResolveSystemAuthContextUseCase resolveSystemAuthContextUseCase;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;
    private final AuditLogger auditLogger;

    public AuthFilter(ResolveAuthContextUseCase resolveAuthContextUseCase,
                      ResolveSystemAuthContextUseCase resolveSystemAuthContextUseCase,
                      JwtProvider jwtProvider,
                      ObjectMapper objectMapper,
                      AuditLogger auditLogger) {
        this.resolveAuthContextUseCase = resolveAuthContextUseCase;
        this.resolveSystemAuthContextUseCase = resolveSystemAuthContextUseCase;
        this.jwtProvider = jwtProvider;
        this.objectMapper = objectMapper;
        this.auditLogger = auditLogger;
    }

    private record PublicRoute(String method, String pattern) {}

    private static final List<PublicRoute> PUBLIC_PATHS = List.of(
            new PublicRoute("POST", "/auth/login/**"),
            new PublicRoute("POST", "/auth/refresh"),
            new PublicRoute("POST", "/register"),
            new PublicRoute("POST", "/register/verify"),
            new PublicRoute("GET",  "/register/suggest-code"),
            new PublicRoute("GET",  "/register/code-availability"),
            new PublicRoute("POST", "/dian/webhooks/**"),
            new PublicRoute("GET",  "/countries"),
            new PublicRoute("GET",  "/countries/{countryId}/states"),
            new PublicRoute("GET",  "/states/{stateId}/cities"),
            new PublicRoute("GET",  "/species/{specieId}/breeds"),
            new PublicRoute("GET",  "/species"),
            new PublicRoute("GET",  "/animal-colors"),
            new PublicRoute("GET",  "/consultation-types"),
            new PublicRoute("GET",  "/modules"),
            new PublicRoute("GET",  "/sub-modules"),
            new PublicRoute("GET",  "/spa-types"),
            new PublicRoute(null,   "/swagger-ui/**"),
            new PublicRoute(null,   "/v3/api-docs/**"),
            new PublicRoute(null,   "/swagger-resources/**"),
            new PublicRoute(null,   "/webjars/**"),
            new PublicRoute(null,   "/actuator/**")
    );

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String method = request.getMethod();
        String path = request.getServletPath();
        return PUBLIC_PATHS.stream().anyMatch(r ->
                (r.method() == null || r.method().equalsIgnoreCase(method))
                        && PATH_MATCHER.match(r.pattern(), path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            writeUnauthorized(request, response, "TOKEN_MISSING", "Missing or invalid Authorization header");
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

        AuthContext authContext = switch (type) {
            case "EMPLOYEE"    -> resolveAuthContextUseCase.execute(id, authVersion);
            case "SYSTEM_USER" -> resolveSystemAuthContextUseCase.execute(id);
            default            -> null;
        };

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

    /** Pone la identidad del actor en el MDC para que aparezca como campos en cada log de la request. */
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
        List<GrantedAuthority> authorities = new ArrayList<>(
            authContext.permissions().stream()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList()
        );
        if (authContext instanceof SystemContext) {
            authorities.add(new SimpleGrantedAuthority(SYSTEM_ROLE));
        }
        return new UsernamePasswordAuthenticationToken(authContext, null, authorities);
    }

    /**
     * Rechazo de autenticación en formato ProblemDetail (RFC 7807), consistente con
     * {@code GlobalExceptionHandler}. El {@code code} discrimina el motivo para que el
     * front decida: {@code TOKEN_EXPIRED} → intentar refrescar; {@code TOKEN_INVALID} /
     * {@code TOKEN_MISSING} → desloguear.
     */
    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response,
                                   String code, String detail) throws IOException {
        auditLogger.unauthenticated(request.getMethod(), request.getRequestURI(), detail);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
        problem.setTitle(HttpStatus.UNAUTHORIZED.getReasonPhrase());
        problem.setProperty("code", code);
        String traceId = MDC.get("traceId");
        if (traceId != null) problem.setProperty("traceId", traceId);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
