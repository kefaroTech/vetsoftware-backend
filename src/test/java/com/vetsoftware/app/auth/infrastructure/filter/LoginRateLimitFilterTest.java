package com.vetsoftware.app.auth.infrastructure.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.auth.infrastructure.config.PublicRoutes;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
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
                "/auth/reset-password", "/register/verify"})
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
}
