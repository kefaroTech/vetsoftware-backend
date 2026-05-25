package com.vetsoftware.app.auth.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.auth.application.dto.SystemContext;
import com.vetsoftware.app.auth.application.port.in.ResolveAuthContextUseCase;
import com.vetsoftware.app.auth.application.port.in.ResolveSystemAuthContextUseCase;
import com.vetsoftware.app.auth.infrastructure.security.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    public AuthFilter(ResolveAuthContextUseCase resolveAuthContextUseCase,
                      ResolveSystemAuthContextUseCase resolveSystemAuthContextUseCase,
                      JwtProvider jwtProvider,
                      ObjectMapper objectMapper) {
        this.resolveAuthContextUseCase = resolveAuthContextUseCase;
        this.resolveSystemAuthContextUseCase = resolveSystemAuthContextUseCase;
        this.jwtProvider = jwtProvider;
        this.objectMapper = objectMapper;
    }

    private record PublicRoute(String method, String pattern) {}

    private static final List<PublicRoute> PUBLIC_PATHS = List.of(
            new PublicRoute("POST", "/auth/login/**"),
            new PublicRoute("POST", "/register"),
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
            case "EMPLOYEE"    -> resolveAuthContextUseCase.execute(id, extractCompanyId(token));
            case "SYSTEM_USER" -> resolveSystemAuthContextUseCase.execute(id);
            default            -> null;
        };

        if (authContext == null) {
            writeUnauthorized(response, "Unknown user type");
            return;
        }

        try {
            SecurityContextHolder.getContext().setAuthentication(toAuthentication(authContext));
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
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

    private String extractType(String token) {
        try {
            return jwtProvider.extractType(token);
        } catch (Exception e) {
            return null;
        }
    }

    private Long extractId(String token) {
        try {
            return jwtProvider.extractId(token);
        } catch (Exception e) {
            return null;
        }
    }

    private Long extractCompanyId(String token) {
        try {
            return jwtProvider.extractCompanyId(token);
        } catch (Exception e) {
            return null;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("error", message));
    }
}
