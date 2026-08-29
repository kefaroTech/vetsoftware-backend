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
            new Route(HttpMethod.POST, "/configurator/resolve"),
            // El catalogo comercial con precio, que es lo que la landing publica
            // necesita para vender. Mismo publico y mismo motivo que el asistente: un
            // prospecto sin cuenta tiene que poder ver cuanto cuesta antes de dar su
            // NIT, y hasta ahora no habia ningun endpoint que se lo dijera -los tres
            // de catalogo estan cerrados a hasRole('SYSTEM')-.
            //
            // Patron literal y NO /plans/**, por lo mismo que /configurator: el mismo
            // prefijo acabara colgando la administracion de planes, y un comodin la
            // abriria al mundo sin que nadie lo vea en el diff.
            //
            // Lo sirve GetPublicPlansUseCase, anotado @NoAuthorizationRequired: son
            // las DOS cosas que hacen publica una ruta aqui, y hacer solo una deja al
            // prospecto con un 401 -si falta esta linea- o con un puerto abierto que
            // nadie puede alcanzar -si falta la anotacion-.
            //
            // Es un GET, asi que no le aplica la invariante de
            // toda_ruta_publica_post_esta_limitada.
            new Route(HttpMethod.GET, "/plans"),
            // El catalogo contratable COMPLETO, que es lo que /plans no puede dar: alli
            // un modulo sale sin precio a proposito, porque el precio es el del paquete
            // que lo contiene. El modelo de compra que quiere el negocio es "solo compre
            // lo que necesite", y para eso el prospecto tiene que ver cuanto cuesta cada
            // pieza por si misma.
            //
            // Ruta aparte y no un campo mas en /plans: PublicPlanCatalogResponse lo
            // consume la landing del tenant con MatchesContract, cuya comprobacion
            // UndeclaredFields falla en cuanto la respuesta trae un campo que el front no
            // declara. Un recurso nuevo es estrictamente aditivo.
            //
            // Patron literal y NO /catalog/**, por lo mismo que /plans y /configurator.
            // Ojo: /catalog-items y /catalog-prices son OTRAS rutas, cerradas a
            // hasRole('SYSTEM'), y un comodin sobre este prefijo no las alcanzaria por
            // AntPathMatcher pero si invitaria al proximo a escribirlo.
            //
            // Lo sirve GetPublicCatalogUseCase, anotado @NoAuthorizationRequired: las
            // DOS cosas, como arriba. Es un GET, asi que no le aplica la invariante de
            // toda_ruta_publica_post_esta_limitada.
            new Route(HttpMethod.GET, "/catalog"),
            // La calculadora de la landing: cuanto costaria una seleccion, sin crear
            // ninguna oferta y sin cuenta. Existe porque la escalera de tramos NO se
            // publica -es la politica de descuento por volumen, y PublicPlanQueryPortIT
            // tiene una prueba que se pone roja si alguien la publica-, asi que un front
            // con solo el tramo de entrada no puede mas que extrapolar: quince usuarios
            // le salen 156.000 y la contratacion cobra 141.000. El servidor devuelve el
            // importe ya calculado con el mismo codigo que congela una oferta real.
            //
            // Es un POST publico, asi que ademas de la ruta y del
            // @NoAuthorizationRequired del puerto lleva su propio limite por IP en
            // LoginRateLimitFilter: toda_ruta_publica_post_esta_limitada recorre esta
            // lista y rompe el build si falta.
            //
            // Patron literal: /quotes/** abriria el embudo comercial entero, que es
            // territorio de SYSTEM y del tenant autenticado.
            new Route(HttpMethod.POST, "/quotes/preview"),
            // Alta de superadministradores de plataforma por invitacion (#360). Las seis
            // son anonimas por construccion, no por comodidad: quien solicita el acceso
            // todavia no tiene cuenta, y quien aprueba, rechaza o acepta se acredita con
            // la posesion de un token de un solo uso —mas un codigo de 6 digitos en los
            // dos primeros—, nunca con un JWT.
            //
            // Se listan una a una, con metodo explicito y patron literal. /platform/**
            // NO vale: el mismo prefijo acabara colgando endpoints SYSTEM de
            // administracion de plataforma, y un comodin los abriria al mundo sin que
            // nadie lo vea en el diff. Es el razonamiento que ya dejo /configurator con
            // sus dos rutas exactas en vez de /configurator/**.
            //
            // Los cuatro POST llevan limite propio en LoginRateLimitFilter —el test
            // toda_ruta_publica_post_esta_limitada lo exige—. Los dos GET de validacion
            // hoy no lo llevan: hueco conocido y acotado en la incidencia #527.
            new Route(HttpMethod.POST, "/platform/access-request"),
            new Route(HttpMethod.GET, "/platform/access-request/validate"),
            new Route(HttpMethod.POST, "/platform/access-request/approve"),
            new Route(HttpMethod.POST, "/platform/access-request/reject"),
            new Route(HttpMethod.GET, "/platform/invitation/validate"),
            new Route(HttpMethod.POST, "/platform/invitation/accept"),
            new Route(null, "/swagger-ui/**"), new Route(null, "/v3/api-docs/**"),
            new Route(null, "/swagger-resources/**"), new Route(null, "/webjars/**"));

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
