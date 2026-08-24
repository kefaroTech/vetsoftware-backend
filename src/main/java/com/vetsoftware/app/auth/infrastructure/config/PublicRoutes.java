package com.vetsoftware.app.auth.infrastructure.config;

import java.util.List;
import java.util.stream.Stream;
import org.springframework.http.HttpMethod;

/**
 * Fuente única de las rutas que no exigen JWT.
 *
 * <p>
 * Existen dos guardianes sobre las mismas rutas y tienen que decir lo mismo: el
 * {@code AuthFilter} (que rechaza la request antes de resolver el contexto de
 * auth) y la {@code SecurityFilterChain} de negocio (que termina en
 * {@code anyRequest().authenticated()}). Si las dos listas se separan, una ruta
 * pública devuelve 403 o —peor— una ruta privada queda abierta. Por eso viven
 * aquí y no duplicadas en cada clase.
 *
 * <p>
 * Los patrones se evalúan con {@code AntPathMatcher} en el filtro y con
 * {@code PathPattern} en Spring Security, ambos sobre el path sin el
 * {@code context-path}. La sintaxis usada ({@code **} y {@code {var\}}) se
 * comporta igual en los dos.
 */
public final class PublicRoutes {

    /**
     * Ruta pública.
     *
     * @param method
     *            método HTTP exigido; {@code null} significa cualquiera
     * @param pattern
     *            patrón de path relativo al {@code context-path}
     */
    public record Route(HttpMethod method, String pattern) {

        public Route {
            if (pattern == null || pattern.isBlank()) {
                throw new IllegalArgumentException("pattern is required");
            }
        }

        /** {@code true} si la ruta acepta cualquier método HTTP. */
        public boolean anyMethod() {
            return method == null;
        }
    }

    /**
     * Rutas públicas servidas por la cadena de negocio: flujos de autenticación
     * previos a tener token, el webhook de la DIAN (autenticado por firma del
     * proveedor, no por JWT), los catálogos maestros que el registro necesita antes
     * de existir como usuario, y la documentación OpenAPI.
     */
    public static final List<Route> BUSINESS = List.of(new Route(HttpMethod.POST, "/auth/login/**"),
            new Route(HttpMethod.POST, "/auth/refresh"), new Route(HttpMethod.POST, "/register"),
            new Route(HttpMethod.POST, "/register/verify"),
            new Route(HttpMethod.POST, "/auth/forgot-password"),
            new Route(HttpMethod.GET, "/auth/reset-password/validate"),
            new Route(HttpMethod.POST, "/auth/reset-password"),
            new Route(HttpMethod.POST, "/auth/recover-code"),
            new Route(HttpMethod.POST, "/dian/webhooks/**"),
            new Route(HttpMethod.GET, "/countries"),
            new Route(HttpMethod.GET, "/countries/{countryId}/states"),
            new Route(HttpMethod.GET, "/states/{stateId}/cities"),
            new Route(HttpMethod.GET, "/species/{specieId}/breeds"),
            new Route(HttpMethod.GET, "/species"), new Route(HttpMethod.GET, "/animal-colors"),
            new Route(HttpMethod.GET, "/consultation-types"), new Route(HttpMethod.GET, "/modules"),
            new Route(HttpMethod.GET, "/sub-modules"), new Route(HttpMethod.GET, "/spa-types"),
            // El asistente de venta lo lee un prospecto que todavia no es cliente: si
            // exigiera token no se podria cotizar antes de existir como usuario. Van
            // las DOS mitades del asistente —leer el cuestionario y resolver lo
            // respondido—: abrir solo la primera deja al prospecto con un 401 en el
            // paso siguiente, que es el unico para el que la primera se abrio.
            // Los patrones son exactos y NO /configurator/**, que abriria tambien los
            // endpoints SYSTEM de administracion del cuestionario que cuelgan del mismo
            // prefijo.
            //
            // /configurator/resolve es un POST anonimo, asi que lleva su propio limite
            // por IP en LoginRateLimitFilter: LoginRateLimitFilterTest exige que toda
            // ruta publica POST este limitada, y esa prueba es lo que hace de esto una
            // invariante y no una buena intencion.
            new Route(HttpMethod.GET, "/configurator/questionnaire"),
            new Route(HttpMethod.POST, "/configurator/resolve"), new Route(null, "/swagger-ui/**"),
            new Route(null, "/v3/api-docs/**"), new Route(null, "/swagger-resources/**"),
            new Route(null, "/webjars/**"));

    /**
     * Rutas que resuelve otra {@code SecurityFilterChain} —hoy solo Actuator, con
     * {@code @Order(1)} y su propia autenticación básica—. La cadena de negocio
     * nunca las ve, así que NO se listan como {@code permitAll} allí; pero el
     * {@code AuthFilter} sí las atraviesa (Spring Boot lo auto-registra además como
     * filtro de servlet) y debe dejarlas pasar sin exigir JWT de negocio.
     */
    public static final List<Route> OTHER_CHAINS = List.of(new Route(null, "/actuator/**"));

    /** Todo lo que el {@code AuthFilter} deja pasar sin validar JWT. */
    public static final List<Route> JWT_EXCLUDED = Stream
            .concat(BUSINESS.stream(), OTHER_CHAINS.stream()).toList();

    private PublicRoutes() {
    }
}
