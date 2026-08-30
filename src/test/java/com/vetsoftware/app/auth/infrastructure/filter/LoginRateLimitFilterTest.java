package com.vetsoftware.app.auth.infrastructure.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.infrastructure.config.PublicRoutes;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
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
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Qué rutas entran al limitador. Lo que se prueba aquí es la <b>selección</b>
 * —{@code shouldNotFilter}—, no el consumo del bucket: eso vive en Redis y no
 * se puede afirmar sin una base real.
 *
 * <p>
 * La prueba que cierra BE-15 es {@code toda_ruta_publica_post_esta_limitada}:
 * recorre {@link PublicRoutes#BUSINESS} en vez de una lista copiada a mano, así
 * que <b>una ruta pública nueva sin límite rompe este test</b>. Es la
 * diferencia entre arreglar los tres huecos de hoy y que no vuelvan a aparecer.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginRateLimitFilter — qué rutas entran al limitador")
class LoginRateLimitFilterTest {

    /**
     * Rutas públicas POST que <b>no</b> necesitan límite propio, con el motivo. Es
     * una lista de excepciones explícitas: lo que no esté aquí tiene que estar
     * limitado.
     */
    private static final Set<String> POST_SIN_LIMITE_JUSTIFICADO = Set.of();

    @Mock
    private LettuceBasedProxyManager<String> proxyManager;
    @Mock
    private AuditLogger auditLogger;

    private LoginRateLimitFilter filter;

    @BeforeEach
    void construirFiltro() {
        filter = new LoginRateLimitFilter(proxyManager, new ObjectMapper(), auditLogger);
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

        @Test
        @DisplayName("toda ruta pública POST está limitada: una nueva sin límite rompe esta prueba")
        void toda_ruta_publica_post_esta_limitada() {
            List<String> sinLimite = PublicRoutes.BUSINESS.stream()
                    .filter(route -> HttpMethod.POST.equals(route.method()))
                    .map(PublicRoutes.Route::pattern).map(LoginRateLimitFilterTest::rutaConcreta)
                    .filter(path -> !POST_SIN_LIMITE_JUSTIFICADO.contains(path))
                    .filter(path -> filter.shouldNotFilter(request("POST", path))).toList();

            assertThat(sinLimite).as("rutas públicas POST sin rate limit").isEmpty();
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

    /** Sustituye los comodines del patrón por un segmento concreto. */
    private static String rutaConcreta(String pattern) {
        return pattern.replace("/**", "/x");
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
}
