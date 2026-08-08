package com.vetsoftware.app.auth.infrastructure.config;

import static org.springframework.security.config.Customizer.withDefaults;

import com.vetsoftware.app.auth.infrastructure.filter.AuthFilter;
import com.vetsoftware.app.auth.infrastructure.security.SecurityProblemDetailHandler;
import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final long HSTS_MAX_AGE_SECONDS = 31_536_000L;

    private static final String PERMISSIONS_POLICY = "camera=(), geolocation=(), microphone=(), "
            + "payment=(), usb=(), interest-cohort=()";

    /**
     * Máximamente restrictiva: esta cadena solo devuelve JSON, no carga ningún
     * recurso. La CSP de las SPA se define en su propio hosting, no aquí.
     */
    private static final String API_CONTENT_SECURITY_POLICY = "default-src 'none'; "
            + "frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

    /**
     * Swagger UI sí carga sus propios scripts, estilos y fuentes desde este mismo
     * origen, así que queda fuera de la CSP de la API; si no, la consola de
     * documentación deja de renderizar.
     */
    private static final List<String> DOCS_PATHS = List.of("/swagger-ui/**", "/v3/api-docs/**",
            "/swagger-resources/**", "/webjars/**");

    private final AuthFilter authFilter;
    private final SecurityProblemDetailHandler problemDetailHandler;

    public SecurityConfig(AuthFilter authFilter,
            SecurityProblemDetailHandler problemDetailHandler) {
        this.authFilter = authFilter;
        this.problemDetailHandler = problemDetailHandler;
    }

    @Bean
    @Order(2)
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(SecurityConfig::authorize)
                .headers(SecurityConfig::securityHeaders)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(problemDetailHandler)
                        .accessDeniedHandler(problemDetailHandler))
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class).build();
    }

    /**
     * Segunda barrera de autorización. El {@code AuthFilter} ya rechaza lo que
     * llega sin token válido; esto cubre el caso de que ese filtro cambie, se
     * reordene o se salte: nada llega al controller sin un principal autenticado
     * salvo lo declarado en {@link PublicRoutes}.
     *
     * <p>
     * Ojo con el alcance: {@code authenticated()} exige identidad, no permiso. Un
     * endpoint nuevo sin {@code @PreAuthorize} sigue siendo alcanzable por
     * cualquier JWT válido — eso lo tiene que atrapar la regla de arquitectura, no
     * esta cadena.
     */
    private static void authorize(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        // Dispatches internos del contenedor. Sin esto, un 404 o una excepción no
        // capturada sobre una ruta pública se re-evalúa sobre el dispatch ERROR y el
        // cliente recibe un 403 en vez del cuerpo de error real.
        auth.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll();

        // El preflight CORS no lleva Authorization. El AuthFilter también lo excluye.
        auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

        PublicRoutes.BUSINESS.forEach(route -> permit(auth, route));

        auth.anyRequest().authenticated();
    }

    private static void permit(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
            PublicRoutes.Route route) {
        if (route.anyMethod()) {
            auth.requestMatchers(route.pattern()).permitAll();
        } else {
            auth.requestMatchers(route.method(), route.pattern()).permitAll();
        }
    }

    private static void securityHeaders(HeadersConfigurer<HttpSecurity> headers) {
        headers.frameOptions(FrameOptionsConfig::deny).contentTypeOptions(withDefaults())
                .referrerPolicy(
                        referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(HSTS_MAX_AGE_SECONDS))
                .permissionsPolicyHeader(permissions -> permissions.policy(PERMISSIONS_POLICY))
                .addHeaderWriter(apiContentSecurityPolicyWriter());
    }

    /** CSP estricta en todo salvo la documentación OpenAPI. */
    private static HeaderWriter apiContentSecurityPolicyWriter() {
        List<RequestMatcher> docs = DOCS_PATHS.stream()
                .<RequestMatcher>map(PathPatternRequestMatcher::pathPattern).toList();
        return new DelegatingRequestMatcherHeaderWriter(
                new NegatedRequestMatcher(new OrRequestMatcher(docs)),
                new ContentSecurityPolicyHeaderWriter(API_CONTENT_SECURITY_POLICY));
    }
}
