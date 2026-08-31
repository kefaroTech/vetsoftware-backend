package com.vetsoftware.app.auth.infrastructure.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.aiproposal.application.usecase.ProposalReader;
import com.vetsoftware.app.aiproposal.infrastructure.ai.ValkeyDailySpendGuard;
import com.vetsoftware.app.auth.infrastructure.config.PublicRoutes;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.shared.ai.ModelPricing;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Qué rutas entran al limitador. Lo que se prueba aquí es la <b>selección</b>
 * —{@code shouldNotFilter}—, no el consumo del bucket: eso vive en Redis y no
 * se puede afirmar sin una base real.
 *
 * <p>
 * La prueba que cierra BE-15 es {@code toda_ruta_publica_esta_limitada}:
 * recorre {@link PublicRoutes#BUSINESS} en vez de una lista copiada a mano, así
 * que <b>una ruta pública nueva sin límite rompe este test</b>. Es la
 * diferencia entre arreglar los tres huecos de hoy y que no vuelvan a aparecer.
 *
 * <p>
 * ⛔ <b>Y recorre TODOS los métodos, no solo los POST.</b> Con el filtro por
 * {@code HttpMethod.POST} que tenía antes, el invariante no veía el
 * {@code PUT /assistant/proposal/lines} —una escritura pública y anónima— ni el
 * {@code GET /assistant/proposal} —que sirve la propuesta entera a quien tenga
 * el token—: sus dos ramas de {@code routeLimit()} se podían borrar y el build
 * seguía verde. Este fichero no contenía la cadena «assistant» ni una vez.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginRateLimitFilter — qué rutas entran al limitador")
class LoginRateLimitFilterTest {

    /**
     * Rutas públicas que <b>no</b> necesitan límite propio, como
     * <code>"MÉTODO /ruta"</code> y con el motivo escrito. Es una lista de
     * excepciones explícitas: lo que no esté aquí tiene que estar limitado.
     *
     * <p>
     * ⛔ <b>Antes esta lista estaba vacía porque la invariante solo recorría los
     * POST</b>, y eso dejaba fuera de todo gate las dos rutas públicas que no lo
     * son: el <code>PUT /assistant/proposal/lines</code> —una escritura anónima— y
     * el <code>GET /assistant/proposal</code> —que sirve la propuesta entera a
     * quien tenga el token—. Sus dos ramas del filtro se podían borrar y el build
     * seguía verde. Ahora la invariante recorre todos los métodos, así que las
     * lecturas de catálogo que legítimamente no necesitan cupo tienen que
     * declararse aquí una por una.
     *
     * <p>
     * Las trece primeras son <b>catálogos públicos de solo lectura</b>: no mandan
     * correo, no consumen ningún token de un solo uso, no cuestan dinero por
     * petición y sirven contenido que es el mismo para todo el mundo. Las tres
     * últimas son las validaciones <code>GET</code> de un token: no lo consumen
     * —solo dicen si sigue vivo— y el POST que sí lo consume ya tiene su propio
     * cupo.
     */
    private static final Set<String> RUTAS_SIN_LIMITE_JUSTIFICADO = Set.of(
            // Catálogos públicos de solo lectura: mismo contenido para todos.
            "GET /countries", "GET /countries/x/states", "GET /states/x/cities", "GET /species",
            "GET /species/x/breeds", "GET /animal-colors", "GET /consultation-types",
            "GET /modules", "GET /sub-modules", "GET /spa-types", "GET /plans", "GET /catalog",
            "GET /legal-documents/x/current",
            // Validaciones de token que NO lo consumen; el POST que sí lo hace ya está
            // limitado.
            "GET /auth/reset-password/validate", "GET /platform/access-request/validate",
            "GET /platform/invitation/validate");

    @Mock
    private LettuceBasedProxyManager<String> proxyManager;
    @Mock
    private AuditLogger auditLogger;

    private LoginRateLimitFilter filter;

    @BeforeEach
    void construirFiltro() {
        filter = filtroConTope(LoginRateLimitFilter.DEFECTO_TOPE_DE_GASTO_DIARIO_USD);
    }

    private LoginRateLimitFilter filtroConTope(String topeUsd) {
        return filtroCon(topeUsd, ModelPricing.DEFECTO_USD_POR_MILLON_ENTRADA,
                ModelPricing.DEFECTO_USD_POR_MILLON_SALIDA);
    }

    /**
     * ⛔ <b>El precio entra por parámetro, y esa es la mitad nueva de la
     * invariante.</b> Antes el coste por llamada era un literal de la propia clase
     * bajo prueba, así que el test solo podía afirmar la relación <i>para el precio
     * de Claude Sonnet</i>. Con un modelo de otra familia —un DeepSeek, un Opus— la
     * aritmética seguía corriendo, callada y equivocada, y ningún test lo veía.
     */
    private LoginRateLimitFilter filtroCon(String topeUsd, String usdPorMillonEntrada,
            String usdPorMillonSalida) {
        return new LoginRateLimitFilter(proxyManager, new ObjectMapper(), auditLogger,
                new BigDecimal(topeUsd), tarifa(usdPorMillonEntrada, usdPorMillonSalida));
    }

    private static ModelPricing tarifa(String usdPorMillonEntrada, String usdPorMillonSalida) {
        return new ModelPricing(new BigDecimal(usdPorMillonEntrada),
                new BigDecimal(usdPorMillonSalida),
                Integer.parseInt(ModelPricing.DEFECTO_TOKENS_ESTIMADOS_ENTRADA),
                Integer.parseInt(ModelPricing.DEFECTO_TOKENS_ESTIMADOS_SALIDA),
                ModelPricing.MODELO_POR_DEFECTO);
    }

    @Nested
    @DisplayName("rutas cubiertas")
    class RutasCubiertas {

        @ParameterizedTest
        @ValueSource(strings = {"/auth/login/employee", "/auth/login/system", "/auth/refresh",
                "/register", "/auth/forgot-password", "/dian/webhooks/matias", "/auth/recover-code",
                "/auth/reset-password", "/register/verify", "/platform/access-request",
                "/platform/access-request/approve", "/platform/access-request/reject",
                "/platform/invitation/accept"})
        @DisplayName("cada ruta pública sensible pasa por el limitador")
        void cada_ruta_publica_sensible_pasa_por_el_limitador(String path) {
            assertThat(filter.shouldNotFilter(request("POST", path))).isFalse();
        }

        /**
         * ⛔ <b>Todos los métodos, no solo POST.</b> Filtrar por {@code HttpMethod.POST}
         * hacía que el invariante no viera ni una sola ruta del asistente distinta del
         * POST: el {@code PUT} que reescribe el carrito y el {@code GET} que sirve la
         * propuesta entera quedaban sin gate, y este fichero no contenía la cadena
         * «assistant» ni una vez. Con esto, borrar cualquiera de las dos ramas de
         * {@code routeLimit()} pone el test en rojo.
         */
        @Test
        @DisplayName("toda ruta pública está limitada, sea cual sea su método: una nueva sin"
                + " límite rompe esta prueba")
        void toda_ruta_publica_esta_limitada() {
            List<String> sinLimite = rutasPublicas()
                    .filter(clave -> !RUTAS_SIN_LIMITE_JUSTIFICADO.contains(clave))
                    .filter(LoginRateLimitFilterTest.this::noPasaPorElLimitador).toList();

            assertThat(sinLimite).as("rutas públicas sin rate limit").isEmpty();
        }

        /**
         * La gemela anti-podredumbre, con la misma forma que las listas de exenciones
         * de ArchUnit: una excepción que ya no corresponde a ninguna ruta pública —o
         * que corresponde a una que entretanto SÍ se limitó— deja el repositorio
         * afirmando por escrito algo falso, y enseña a no leer la lista.
         */
        @Test
        @DisplayName("ninguna excepción está podrida: todas apuntan a una ruta pública que sigue"
                + " sin límite")
        void ninguna_excepcion_esta_podrida() {
            Set<String> publicas = rutasPublicas().collect(java.util.stream.Collectors.toSet());

            assertThat(RUTAS_SIN_LIMITE_JUSTIFICADO).as("excepciones que ya no son rutas públicas")
                    .isSubsetOf(publicas);
            assertThat(RUTAS_SIN_LIMITE_JUSTIFICADO)
                    .as("excepciones que hoy SÍ están limitadas y sobran")
                    .allMatch(LoginRateLimitFilterTest.this::noPasaPorElLimitador);
        }

        @Test
        @DisplayName("las dos rutas del asistente que no son POST sí pasan por el limitador")
        void las_rutas_no_post_del_asistente_estan_limitadas() {
            assertThat(filter.shouldNotFilter(request("PUT", "/assistant/proposal/lines")))
                    .isFalse();
            assertThat(filter.shouldNotFilter(request("GET", "/assistant/proposal"))).isFalse();
        }
    }

    /** Las rutas públicas con método declarado, como {@code "MÉTODO /ruta"}. */
    private static java.util.stream.Stream<String> rutasPublicas() {
        return PublicRoutes.BUSINESS.stream().filter(route -> route.method() != null)
                .map(route -> route.method().name() + " " + rutaConcreta(route.pattern()));
    }

    private boolean noPasaPorElLimitador(String metodoYRuta) {
        int espacio = metodoYRuta.indexOf(' ');
        return filter.shouldNotFilter(
                request(metodoYRuta.substring(0, espacio), metodoYRuta.substring(espacio + 1)));
    }

    @Nested
    @DisplayName("la ruta concreta con la que se prueba cada patrón")
    class RutaConcretaDelPatron {

        @Test
        @DisplayName("expande el comodín y también la variable de path")
        void expande_el_comodin_y_tambien_la_variable() {
            assertThat(rutaConcreta("/auth/login/**")).isEqualTo("/auth/login/x");
            assertThat(rutaConcreta("/legal-documents/{code}/current"))
                    .isEqualTo("/legal-documents/x/current");
            assertThat(rutaConcreta("/a/{uno}/b/{dos}")).isEqualTo("/a/x/b/x");
            assertThat(rutaConcreta("/quotes/preview")).isEqualTo("/quotes/preview");
        }

        @Test
        @DisplayName("ninguna ruta pública produce un path que no casaría con nada")
        void ninguna_ruta_publica_produce_un_path_imposible() {
            assertThat(PublicRoutes.BUSINESS.stream().map(PublicRoutes.Route::pattern)
                    .map(LoginRateLimitFilterTest::rutaConcreta).toList())
                    .allSatisfy(path -> assertThat(path).doesNotContain("{").doesNotContain("}")
                            .doesNotContain("*"));
        }
    }

    @Nested
    @DisplayName("rutas que no debe tocar")
    class RutasIgnoradas {

        @Test
        @DisplayName("solo POST: el mismo path en GET no consume cupo")
        void solo_post() {
            assertThat(filter.shouldNotFilter(request("GET", "/auth/login/employee"))).isTrue();
        }

        @Test
        @DisplayName("la validación GET del token de reseteo no la atrapa el límite del POST")
        void la_validacion_get_no_la_atrapa_el_limite_del_post() {
            assertThat(filter.shouldNotFilter(request("GET", "/auth/reset-password/validate")))
                    .isTrue();
            assertThat(filter.shouldNotFilter(request("POST", "/auth/reset-password/validate")))
                    .isTrue();
        }

        @Test
        @DisplayName("los GET de validación del alta de plataforma no los atrapa el límite del POST")
        void los_get_de_validacion_de_plataforma_no_los_atrapa_el_limite_del_post() {
            assertThat(filter.shouldNotFilter(request("GET", "/platform/access-request/validate")))
                    .isTrue();
            assertThat(filter.shouldNotFilter(request("GET", "/platform/invitation/validate")))
                    .isTrue();
            // La razón por la que las ramas nuevas usan equals y no startsWith: con
            // startsWith, esta subruta caería en el bucket de /platform/access-request
            // —3/h, el que protege el correo al aprobador— y lo agotaría desde fuera.
            assertThat(filter.shouldNotFilter(request("POST", "/platform/access-request/validate")))
                    .isTrue();
        }

        @Test
        @DisplayName("una ruta autenticada no pasa por el limitador")
        void una_ruta_autenticada_no_pasa() {
            assertThat(filter.shouldNotFilter(request("POST", "/auth/logout"))).isTrue();
        }

        @Test
        @DisplayName("el prefijo del webhook sin proveedor no es una ruta del limitador")
        void el_prefijo_del_webhook_sin_proveedor_no_cuenta() {
            assertThat(filter.shouldNotFilter(request("POST", "/dian/webhooks"))).isTrue();
        }
    }

    /**
     * Variable de path de Spring: <code>{nombre}</code>. Deja fuera a propósito la
     * forma con regex incrustada (<code>&#123;id:\d+&#125;</code>), que este
     * proyecto no usa en ninguna ruta pública y cuyas llaves internas romperían el
     * predicado — si algún día aparece una, la comprobación de abajo se pone roja
     * en vez de dejarla pasar en silencio.
     */
    private static final Pattern VARIABLE_DE_PATH = Pattern.compile("\\{[^/{}]+}");

    /**
     * Sustituye por un segmento concreto <b>todo</b> lo que el patrón deja abierto:
     * el comodín <code>/**</code> y cada variable de path.
     *
     * <p>
     * <b>Expandir la variable no es cosmético.</b> {@code routeLimit()} casa el
     * path crudo con {@code equals} y {@code startsWith}, así que
     * {@code "/x/{token}/refine"} no casa con nada: si esto devolviera el patrón
     * con las llaves puestas, {@code toda_ruta_publica_post_esta_limitada} <b>nunca
     * podría satisfacerse</b> para una ruta con variable. Quien declarara su
     * {@code RouteLimit} correctamente vería el gate en rojo, y la salida que
     * tendría a mano —meter la ruta en {@code POST_SIN_LIMITE_JUSTIFICADO}— es
     * renunciar a la invariante para que pase. Con la expansión, el test vuelve a
     * medir lo que dice medir.
     *
     * <p>
     * Hoy ninguna ruta pública POST lleva variable —las cuatro del asistente mueven
     * su token a {@code ?token=} y al cuerpo justo para no llevarla—, así que esta
     * corrección no cambia ni un resultado. Se hace igualmente: la primera ruta
     * pública con variable ya está aquí ({@code GET
     * /legal-documents/&#123;code&#125;/current}), y dejar el test insatisfacible
     * para el siguiente que abra un POST es peor que dejarlo rojo.
     */
    private static String rutaConcreta(String pattern) {
        return VARIABLE_DE_PATH.matcher(pattern.replace("/**", "/x")).replaceAll("x");
    }

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }

    private static MockHttpServletRequest requestConCuerpo(String method, String path,
            String body) {
        MockHttpServletRequest request = request(method, path);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    /**
     * Consumo real del cupo: sustituye el proxy de Redis por un bucket doblado. Lo
     * que se prueba aquí es la <b>orquestación</b> del filtro —qué claves consume,
     * en qué orden, y qué hace con el resultado—, no el algoritmo de bucket4j en
     * sí.
     */
    @Nested
    @DisplayName("consumo del cupo")
    class ConsumoDelCupo {

        @Mock
        private RemoteBucketBuilder<String> remoteBucketBuilder;
        @Mock
        private BucketProxy bucket;
        @Mock
        private FilterChain chain;

        @BeforeEach
        void enrutarElProxyHaciaElBucketDoblado() {
            // lenient(): el caso "ruta sin límite" no consume ningún cupo, así que este
            // doblado queda sin usar ahí — es scaffolding compartido, no un contrato del
            // caso concreto.
            org.mockito.Mockito.lenient().when(proxyManager.builder())
                    .thenReturn(remoteBucketBuilder);
            // build(K, Supplier) y build(K, BucketConfiguration) son ambiguos para any():
            // se fija el tipo del matcher para que el compilador elija el overload
            // correcto.
            org.mockito.Mockito.lenient().when(remoteBucketBuilder.build(anyString(),
                    org.mockito.ArgumentMatchers.<java.util.function.Supplier<io.github.bucket4j.BucketConfiguration>>any()))
                    .thenReturn(bucket);
        }

        @Test
        @DisplayName("sin cupo por IP responde 429 y no llega a la cadena")
        void sin_cupo_por_ip_responde_429() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(false);
            MockHttpServletRequest request = requestConCuerpo("POST", "/auth/login/employee",
                    "{\"employeeCode\":\"EMP-1\",\"password\":\"x\"}");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getHeader("Retry-After")).isNotNull();
            verify(auditLogger).rateLimited("LOGIN_RATE_LIMITED");
            verifyNoInteractions(chain);
        }

        @Test
        @DisplayName("con cupo de IP pero sin cupo por cuenta responde 429 igual")
        void sin_cupo_por_cuenta_responde_429() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(true, false);
            MockHttpServletRequest request = requestConCuerpo("POST", "/auth/login/employee",
                    "{\"employeeCode\":\"EMP-1\",\"password\":\"x\"}");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(429);
            verifyNoInteractions(chain);
        }

        @Test
        @DisplayName("con cupo de sobra la request llega a la cadena con el cuerpo cacheado y legible")
        void con_cupo_llega_a_la_cadena_con_el_cuerpo_cacheado() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(true);
            String cuerpo = "{\"employeeCode\":\"EMP-1\",\"password\":\"x\"}";
            MockHttpServletRequest request = requestConCuerpo("POST", "/auth/login/employee",
                    cuerpo);
            MockHttpServletResponse response = new MockHttpServletResponse();
            ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor
                    .forClass(HttpServletRequest.class);

            filter.doFilterInternal(request, response, chain);

            verify(chain).doFilter(captor.capture(), eq(response));
            HttpServletRequest wrapped = captor.getValue();
            int bytes = cuerpo.getBytes(StandardCharsets.UTF_8).length;
            assertThat(wrapped.getContentLength()).isEqualTo(bytes);
            assertThat(wrapped.getContentLengthLong()).isEqualTo((long) bytes);
            ServletInputStream stream = wrapped.getInputStream();
            stream.setReadListener(null);
            assertThat(stream.isReady()).isTrue();
            byte[] leido = stream.readAllBytes();
            assertThat(new String(leido, StandardCharsets.UTF_8)).isEqualTo(cuerpo);
            assertThat(stream.isFinished()).isTrue();
            assertThat(wrapped.getReader().readLine()).isEqualTo(cuerpo);
        }

        @Test
        @DisplayName("un cuerpo mayor al límite responde 413 antes de tocar la cadena")
        void cuerpo_demasiado_grande_responde_413() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(true);
            String enorme = "a".repeat(16 * 1024 + 1);
            MockHttpServletRequest request = requestConCuerpo("POST", "/auth/login/employee",
                    enorme);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(413);
            verifyNoInteractions(chain);
        }

        @Test
        @DisplayName("un cuerpo JSON inválido no revienta el filtro: sigue con el límite de IP ya consumido")
        void cuerpo_json_invalido_no_revienta_el_filtro() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(true);
            MockHttpServletRequest request = requestConCuerpo("POST", "/auth/login/employee",
                    "{no-es-json");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, chain);

            verify(chain).doFilter(any(), eq(response));
        }

        @Test
        @DisplayName("un webhook de DIAN sin proveedor en la ruta no genera clave de cuenta")
        void webhook_sin_proveedor_no_genera_clave_de_cuenta() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(true);
            MockHttpServletRequest request = request("POST", "/dian/webhooks/");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, chain);

            verify(chain).doFilter(any(), eq(response));
        }

        @Test
        @DisplayName("un webhook de DIAN con proveedor también limita por proveedor")
        void webhook_con_proveedor_limita_por_proveedor() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(false);
            MockHttpServletRequest request = request("POST", "/dian/webhooks/matias");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(429);
        }

        /**
         * Que las cuatro rutas del alta de plataforma estén limitadas no basta: hay que
         * demostrar que están limitadas <b>por separado</b>. Tres de las cuatro cuelgan
         * del mismo prefijo textual, así que un {@code startsWith} las metería en el
         * mismo bucket sin que ninguna prueba de selección se enterase. El código de
         * auditoría es la única señal observable de qué {@code RouteLimit} se eligió.
         */
        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({"/platform/access-request,PLATFORM_ACCESS_REQUEST_RATE_LIMITED",
                "/platform/access-request/approve,PLATFORM_ACCESS_APPROVE_RATE_LIMITED",
                "/platform/access-request/reject,PLATFORM_ACCESS_REJECT_RATE_LIMITED",
                "/platform/invitation/accept,PLATFORM_INVITATION_ACCEPT_RATE_LIMITED"})
        @DisplayName("cada ruta del alta de plataforma agota su propio cupo, no el del vecino")
        void cada_ruta_del_alta_de_plataforma_tiene_su_propio_cupo(String path, String codigo)
                throws Exception {
            when(bucket.tryConsume(1)).thenReturn(false);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request("POST", path), response, chain);

            assertThat(response.getStatus()).isEqualTo(429);
            verify(auditLogger).rateLimited(codigo);
            verifyNoInteractions(chain);
        }

        @Test
        @DisplayName("una ruta sin límite no consume cupo y pasa directo a la cadena")
        void ruta_sin_limite_pasa_directo() throws Exception {
            MockHttpServletRequest request = request("GET", "/animals");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, chain);

            verify(chain).doFilter(request, response);
            verifyNoInteractions(proxyManager);
        }

        @Test
        @DisplayName("un proveedor válido en el webhook agota su propio cupo, aunque el de IP tenga margen")
        void webhook_con_proveedor_valido_agota_su_propio_cupo() throws Exception {
            // IP con margen (true), cupo del proveedor agotado (false): la única forma
            // de distinguir esta rama de "sin cupo por IP" es que el primer tryConsume
            // pase.
            when(bucket.tryConsume(1)).thenReturn(true, false);
            MockHttpServletRequest request = request("POST", "/dian/webhooks/matias");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(429);
            verify(auditLogger).rateLimited("DIAN_WEBHOOK_RATE_LIMITED");
            verifyNoInteractions(chain);
        }

        @Test
        @DisplayName("una subruta tras el proveedor no genera una clave de cuenta propia: solo limita por IP")
        void subruta_tras_el_proveedor_no_genera_clave_de_cuenta() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(true);
            MockHttpServletRequest request = request("POST", "/dian/webhooks/matias/extra");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, chain);

            verify(chain).doFilter(any(), eq(response));
            verify(remoteBucketBuilder, org.mockito.Mockito.times(1)).build(anyString(),
                    org.mockito.ArgumentMatchers.<java.util.function.Supplier<io.github.bucket4j.BucketConfiguration>>any());
        }

        @Test
        @DisplayName("un cuerpo vacío en una ruta con campos de cuenta no genera ninguna clave: solo limita por IP")
        void cuerpo_vacio_no_genera_claves_de_cuenta() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(true);
            MockHttpServletRequest request = request("POST", "/register");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, chain);

            verify(chain).doFilter(any(), eq(response));
            verify(remoteBucketBuilder, org.mockito.Mockito.times(1)).build(anyString(),
                    org.mockito.ArgumentMatchers.<java.util.function.Supplier<io.github.bucket4j.BucketConfiguration>>any());
        }

        @Test
        @DisplayName("un campo no-string o vacío en el cuerpo se ignora sin romper el filtro")
        void campos_invalidos_en_el_cuerpo_se_ignoran() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(true);
            MockHttpServletRequest request = requestConCuerpo("POST", "/register",
                    "{\"employeeEmail\":123,\"companyIdentifier\":\"   \"}");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, chain);

            verify(chain).doFilter(any(), eq(response));
            verify(remoteBucketBuilder, org.mockito.Mockito.times(1)).build(anyString(),
                    org.mockito.ArgumentMatchers.<java.util.function.Supplier<io.github.bucket4j.BucketConfiguration>>any());
        }

        @Test
        @DisplayName("un campo opaco no se normaliza a minúsculas: mayúsculas y minúsculas son cuentas distintas")
        void campo_opaco_no_se_normaliza_a_minusculas() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(true);
            ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);

            filter.doFilterInternal(
                    requestConCuerpo("POST", "/auth/refresh", "{\"refreshToken\":\"ABC\"}"),
                    new MockHttpServletResponse(), chain);
            filter.doFilterInternal(
                    requestConCuerpo("POST", "/auth/refresh", "{\"refreshToken\":\"abc\"}"),
                    new MockHttpServletResponse(), chain);

            verify(remoteBucketBuilder, org.mockito.Mockito.atLeastOnce()).build(keys.capture(),
                    org.mockito.ArgumentMatchers.<java.util.function.Supplier<io.github.bucket4j.BucketConfiguration>>any());
            List<String> clavesDeCuenta = keys.getAllValues().stream()
                    .filter(k -> k.contains("account:")).distinct().toList();
            assertThat(clavesDeCuenta).hasSize(2);
        }

        @Test
        @DisplayName("un campo normal sí se normaliza a minúsculas: mayúsculas y minúsculas son la misma cuenta")
        void campo_normal_se_normaliza_a_minusculas() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(true);
            ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);

            filter.doFilterInternal(
                    requestConCuerpo("POST", "/register",
                            "{\"employeeEmail\":\"a@b.com\",\"companyIdentifier\":\"ABC\"}"),
                    new MockHttpServletResponse(), chain);
            filter.doFilterInternal(
                    requestConCuerpo("POST", "/register",
                            "{\"employeeEmail\":\"a@b.com\",\"companyIdentifier\":\"abc\"}"),
                    new MockHttpServletResponse(), chain);

            verify(remoteBucketBuilder, org.mockito.Mockito.atLeastOnce()).build(keys.capture(),
                    org.mockito.ArgumentMatchers.<java.util.function.Supplier<io.github.bucket4j.BucketConfiguration>>any());
            List<String> clavesDeCuenta = keys.getAllValues().stream()
                    .filter(k -> k.contains("account:")).distinct().toList();
            assertThat(clavesDeCuenta).hasSize(2);
        }

        /**
         * ⛔ <b>El refinamiento paga modelo igual que la propuesta inicial y no contaba
         * nada.</b> Su {@code RouteLimit} se construía con el constructor de siete
         * argumentos, así que {@code daily} quedaba a {@code null},
         * {@code dailyPerIp()} devolvía 0 y {@code consumirDiario} cortocircuitaba a
         * {@code true}. El código que lo dejaba pasar se lee como una guarda correcta,
         * que es lo que lo hacía invisible.
         */
        @Test
        @DisplayName("el refinamiento tiene cupo diario por IP: agotarlo es 429 aunque la ventana"
                + " horaria tenga margen")
        void el_refinamiento_tiene_cupo_diario() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(true, false);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request("POST", "/assistant/proposal/refine"), response, chain);

            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getHeader("Retry-After"))
                    .as("la ventana que se agotó es la diaria, no la horaria")
                    .isEqualTo(String.valueOf(java.time.Duration.ofDays(1).toSeconds()));
            verify(auditLogger).rateLimited("AI_PROPOSAL_REFINE_RATE_LIMITED");
            verifyNoInteractions(chain);
        }

        /**
         * ⛔ <b>El cubo «global» no era global.</b> Su clave se construía como
         * {@code routeLimit.keyPrefix() + "day:" + "global"}, así que había uno por
         * ruta: las dos que gastan del mismo presupuesto contaban por separado y el
         * techo efectivo de la plataforma era el doble del declarado.
         */
        @Test
        @DisplayName("las dos rutas que pagan comparten un único contador diario, fuera del"
                + " prefijo de su ruta")
        void las_dos_rutas_que_pagan_comparten_el_contador() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(true);
            ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);

            filter.doFilterInternal(
                    requestConCuerpo("POST", "/assistant/proposal", "{\"email\":\"a@b.com\"}"),
                    new MockHttpServletResponse(), chain);
            List<String> deLaInicial = clavesConsumidas(keys);

            filter.doFilterInternal(request("POST", "/assistant/proposal/refine"),
                    new MockHttpServletResponse(), chain);
            List<String> deLasDos = clavesConsumidas(keys);

            assertThat(deLaInicial).contains(LoginRateLimitFilter.CLAVE_DIARIA_GLOBAL_DE_PAGO);
            assertThat(deLasDos).contains(LoginRateLimitFilter.CLAVE_DIARIA_GLOBAL_DE_PAGO);
            assertThat(deLasDos).as("ninguna clave del presupuesto lleva prefijo de ruta")
                    .noneMatch(clave -> clave.endsWith("day:global")
                            && !clave.equals(LoginRateLimitFilter.CLAVE_DIARIA_GLOBAL_DE_PAGO));
        }

        /**
         * ⛔ <b>Lo que no puede pasar {@code @Valid} no gasta presupuesto.</b> El filtro
         * corre antes que el binder: un cuerpo sin {@code email} —campo
         * {@code @NotBlank @Email}— consumía el cubo de la plataforma y solo después se
         * rechazaba con un 400. Peticiones inválidas, gratis para quien las manda,
         * quemando el cupo de todos.
         */
        @Test
        @DisplayName("una petición sin el campo obligatorio no toca el presupuesto de la"
                + " plataforma")
        void una_peticion_sin_campo_obligatorio_no_toca_el_presupuesto() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(true);
            ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);

            filter.doFilterInternal(requestConCuerpo("POST", "/assistant/proposal", "{}"),
                    new MockHttpServletResponse(), chain);

            assertThat(clavesConsumidas(keys))
                    .doesNotContain(LoginRateLimitFilter.CLAVE_DIARIA_GLOBAL_DE_PAGO);
        }

        @Test
        @DisplayName("y con el campo obligatorio presente sí lo toca: la guarda no rechaza de más")
        void con_el_campo_obligatorio_si_toca_el_presupuesto() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(true);
            ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);

            filter.doFilterInternal(
                    requestConCuerpo("POST", "/assistant/proposal", "{\"email\":\"a@b.com\"}"),
                    new MockHttpServletResponse(), chain);

            assertThat(clavesConsumidas(keys))
                    .contains(LoginRateLimitFilter.CLAVE_DIARIA_GLOBAL_DE_PAGO);
        }

        /**
         * La red que cubre lo que la guarda anterior no ve: el refinamiento no declara
         * campos de cuenta a propósito, así que un cuerpo inválido pasaba y se cobraba
         * igual. Un 400 aguas abajo significa que nunca llegó al caso de uso.
         */
        @Test
        @DisplayName("un 400 aguas abajo devuelve el token al presupuesto: no hubo llamada al"
                + " modelo que pagar")
        void un_400_devuelve_el_token_al_presupuesto() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(true);
            respondeConEstado(400);

            filter.doFilterInternal(request("POST", "/assistant/proposal/refine"),
                    new MockHttpServletResponse(), chain);

            verify(bucket).addTokens(1);
        }

        @Test
        @DisplayName("un 500 NO devuelve el token: ahí la llamada al modelo pudo ocurrir")
        void un_500_no_devuelve_el_token() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(true);
            respondeConEstado(500);

            filter.doFilterInternal(request("POST", "/assistant/proposal/refine"),
                    new MockHttpServletResponse(), chain);

            verify(bucket, org.mockito.Mockito.never()).addTokens(1);
        }

        /**
         * ⛔ <b>El PUT anónimo no tenía ninguna cota de tamaño.</b> La lectura acotada a
         * 16 KB era un efecto colateral de necesitar el JSON para sacar la clave de
         * cuenta, y esta ruta no declara ninguna, así que el único techo era el del
         * contenedor. Cada código del cuerpo es una fila de {@code ai_proposal_lines}.
         */
        @Test
        @DisplayName("el PUT de líneas también tiene cota de cuerpo: 16 KB y un byte es 413")
        void el_put_de_lineas_tiene_cota_de_cuerpo() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(true);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(
                    requestConCuerpo("PUT", "/assistant/proposal/lines", "a".repeat(16 * 1024 + 1)),
                    response, chain);

            assertThat(response.getStatus()).isEqualTo(413);
            verifyNoInteractions(chain);
        }

        private List<String> clavesConsumidas(ArgumentCaptor<String> keys) {
            verify(remoteBucketBuilder, org.mockito.Mockito.atLeastOnce()).build(keys.capture(),
                    org.mockito.ArgumentMatchers.<java.util.function.Supplier<io.github.bucket4j.BucketConfiguration>>any());
            return keys.getAllValues();
        }

        private void respondeConEstado(int estado) throws Exception {
            org.mockito.Mockito.doAnswer(invocacion -> {
                ((MockHttpServletResponse) invocacion.getArgument(1)).setStatus(estado);
                return null;
            }).when(chain).doFilter(any(), any());
        }

        @Test
        @DisplayName("el flujo cacheado se puede leer byte a byte y sabe si aún no ha terminado")
        void el_flujo_cacheado_se_lee_byte_a_byte_y_sabe_si_no_ha_terminado() throws Exception {
            when(bucket.tryConsume(1)).thenReturn(true);
            String cuerpo = "{\"employeeCode\":\"EMP-1\",\"password\":\"x\"}";
            MockHttpServletRequest request = requestConCuerpo("POST", "/auth/login/employee",
                    cuerpo);
            MockHttpServletResponse response = new MockHttpServletResponse();
            ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor
                    .forClass(HttpServletRequest.class);

            filter.doFilterInternal(request, response, chain);

            verify(chain).doFilter(captor.capture(), eq(response));
            ServletInputStream stream = captor.getValue().getInputStream();

            assertThat(stream.isFinished()).isFalse();
            assertThat(stream.read()).isEqualTo((int) cuerpo.charAt(0));
        }
    }

    /**
     * ⛔ <b>La invariante que la auditoría pidió, y no es «el número es 20».</b> Los
     * dos límites del asistente —el de dinero y el de peticiones— se elegían por
     * separado y quedaron calibrados al revés: 0,0176 USD por llamada contra un
     * tope de 0,33 USD son <b>dieciocho</b> invocaciones al día para toda la
     * plataforma, y el cupo por IP declaraba <b>veinte</b>. Cada número se leía
     * bien por su cuenta; lo que estaba mal era la relación entre los dos, y ningún
     * test que fijara un número la habría visto.
     */
    @Nested
    @DisplayName("El límite por IP se deriva del límite de dinero")
    class ElLimitePorIpSeDerivaDelDinero {

        /**
         * ⛔ <b>Y ahora el precio también es un parámetro.</b> La versión anterior
         * recorría ocho topes contra <i>un solo</i> precio —el de Sonnet, compilado
         * dentro del filtro—, así que afirmaba la relación para el modelo de hoy y para
         * ninguno más. Aquí van tres familias con órdenes de magnitud distintos: la
         * cara (15/75 USD por millón), la de hoy (2/10) y una barata (0,14/0,28). La
         * invariante tiene que aguantarlas todas, porque lo que se está probando es la
         * aritmética, no la cifra.
         *
         * <p>
         * <b>El presupuesto de cada caso financia al menos una sesión por origen</b>, y
         * no es un detalle: por debajo de eso entra el suelo deliberado de
         * {@code limitesDePago}, que sube el cupo por IP al mínimo útil precisamente
         * porque ahí quien corta es el guardián de gasto y no este filtro. Esa rama la
         * cubre {@code un_tope_de_juguete_no_deja_cupos_a_cero}; mezclarlas aquí haría
         * que la invariante fuese falsa por diseño.
         */
        @ParameterizedTest(name = "tope {0} USD con tarifas {1}/{2} USD por millón")
        @CsvSource({"0.33, 2, 10", "0.50, 2, 10", "1.00, 2, 10", "5.00, 2, 10", "100.00, 2, 10",
                "2.50, 15, 75", "10.00, 15, 75", "50.00, 15, 75", "0.05, 0.14, 0.28",
                "0.33, 0.14, 0.28", "1.00, 0.14, 0.28", "0.33, 0.05, 0.10"})
        @DisplayName("lo que una IP puede gastar en las dos rutas de pago nunca supera lo que el"
                + " tope financia, sea cual sea el precio del modelo")
        void el_cupo_por_ip_nunca_supera_lo_que_el_tope_financia(String tope, String usdEntrada,
                String usdSalida) {
            LoginRateLimitFilter conTope = filtroCon(tope, usdEntrada, usdSalida);

            assertThat(conTope.llamadasDePagoQueFinanciaElTope())
                    .as("el caso tiene que estar por encima del suelo de una sesión por origen, o"
                            + " la invariante no aplica y el test estaría comprobando otra cosa")
                    .isGreaterThanOrEqualTo(LoginRateLimitFilter.ORIGENES_PARA_VACIAR_EL_DIA
                            * LoginRateLimitFilter.LLAMADAS_DE_PAGO_POR_SESION);
            assertThat(conTope.cupoDiarioPorIpDeLasRutasDePago())
                    .as("llamadas de pago que una IP puede consumir en un día")
                    .isLessThanOrEqualTo(conTope.llamadasDePagoQueFinanciaElTope());
        }

        /**
         * La otra mitad de «si alguien cambia uno de los dos, algo lo note»: con un
         * literal, el cupo por IP sería el mismo para cualquier presupuesto.
         */
        @Test
        @DisplayName("subir el tope sube el cupo por IP: el número no es un literal")
        void subir_el_tope_sube_el_cupo() {
            assertThat(filtroConTope("5.00").cupoDiarioPorIpDeLasRutasDePago())
                    .isGreaterThan(filtroConTope("0.33").cupoDiarioPorIpDeLasRutasDePago());
        }

        /**
         * ⛔ <b>La prueba que exige que el precio sea de verdad configuración.</b> Con
         * el mismo presupuesto, un modelo diez veces más caro tiene que dejar menos
         * llamadas por IP y uno diez veces más barato tiene que dejar más. Si alguien
         * devuelve el coste por llamada a un literal —o lo cablea a las tarifas de
         * Sonnet— los tres números se igualan y esto se pone rojo, que es exactamente
         * el fallo que hoy no avisaba: cortar por un número de llamadas calculado con
         * un precio que ya no existe.
         */
        @Test
        @DisplayName("cambiar el precio del modelo cambia el cupo por IP en consecuencia")
        void un_modelo_mas_caro_deja_menos_cupo_por_ip() {
            int conSonnet = filtroCon("5.00", "2", "10").cupoDiarioPorIpDeLasRutasDePago();
            int diezVecesMasCaro = filtroCon("5.00", "20", "100").cupoDiarioPorIpDeLasRutasDePago();
            int diezVecesMasBarato = filtroCon("5.00", "0.2", "1")
                    .cupoDiarioPorIpDeLasRutasDePago();

            assertThat(diezVecesMasCaro).as("un modelo más caro financia menos invocaciones")
                    .isLessThan(conSonnet);
            assertThat(diezVecesMasBarato).as("uno más barato financia más")
                    .isGreaterThan(conSonnet);
        }

        /**
         * Entrada y salida son dos números y no uno, y esto lo hace exigible: mover
         * <i>solo</i> una de las dos tarifas tiene que mover el cupo. Un cálculo que
         * ignorase cualquiera de las dos pasaría el test de arriba y caería aquí.
         */
        @Test
        @DisplayName("la tarifa de entrada y la de salida cuentan por separado")
        void las_dos_tarifas_cuentan_por_separado() {
            int base = filtroCon("5.00", "2", "10").cupoDiarioPorIpDeLasRutasDePago();

            assertThat(filtroCon("5.00", "20", "10").cupoDiarioPorIpDeLasRutasDePago())
                    .as("subir solo la entrada").isLessThan(base);
            assertThat(filtroCon("5.00", "2", "100").cupoDiarioPorIpDeLasRutasDePago())
                    .as("subir solo la salida").isLessThan(base);
        }

        @Test
        @DisplayName("con el tope por defecto, 0,33 USD financian 18 invocaciones")
        void el_tope_por_defecto_financia_dieciocho() {
            assertThat(filter.llamadasDePagoQueFinanciaElTope()).isEqualTo(18);
        }

        /**
         * Un tope que no financia ni una sesión por origen es una configuración de
         * juguete: quien corta ahí es el guardián de gasto, que es fail-closed. El
         * filtro deja el mínimo útil en vez de dejar cupos a cero, que en
         * {@code consumirDiario} significan «sin límite» y serían lo contrario.
         */
        @ParameterizedTest(name = "tope {0} USD")
        @ValueSource(strings = {"0.01", "0.0001", "0"})
        @DisplayName("un tope de juguete no deja ningún cupo a cero, que significaría «sin límite»")
        void un_tope_de_juguete_no_deja_cupos_a_cero(String tope) {
            LoginRateLimitFilter conTope = filtroConTope(tope);

            assertThat(conTope.cupoDiarioPorIpDeLasRutasDePago()).as("cupo por IP").isPositive();
            assertThat(conTope.cupoDiarioGlobal())
                    .as("el techo de la plataforma se apagaría justo cuando no hay presupuesto")
                    .isPositive();
        }

        @Test
        @DisplayName("el tope por defecto que declara el filtro es el que aplica el guardián")
        void el_tope_declarado_es_el_que_se_aplica() {
            assertThat(LoginRateLimitFilter.DEFECTO_TOPE_DE_GASTO_DIARIO_USD)
                    .isEqualTo(ValkeyDailySpendGuard.DEFECTO_TOPE_DIARIO_USD);
        }

        @Test
        @DisplayName("una sesión son las mismas cuatro llamadas de pago que declara el dominio")
        void una_sesion_son_cuatro_llamadas() {
            assertThat(LoginRateLimitFilter.LLAMADAS_DE_PAGO_POR_SESION)
                    .isEqualTo(ProposalReader.MAX_TURNOS_DE_MODELO);
        }
    }
}
