package com.vetsoftware.app.auth.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;

/**
 * Esta clase es la lista de rutas que se sirven SIN autenticar, asi que aqui la
 * asercion no es un detalle de estilo: es el inventario. Hasta #126 el glob de
 * JaCoCo sobre el paquete de configuracion la sacaba del denominador, y
 * {@code OTHER_CHAINS} no tenia ni una linea de test: abrir un patron de mas no
 * rompia absolutamente nada.
 *
 * <p>
 * El listado completo se afirma a proposito, aunque sea largo: quien anada una
 * ruta publica se encuentra este test en rojo y tiene que escribirla tambien
 * aqui. Eso convierte «abrir un endpoint al mundo» en una decision visible en
 * el diff, que es justo lo que faltaba.
 */
@DisplayName("PublicRoutes — el inventario de lo que se sirve sin JWT")
class PublicRoutesTest {

    /** Forma canonica «METODO patron» para comparar rutas de un vistazo. */
    private static String describe(PublicRoutes.Route route) {
        return (route.anyMethod() ? "ANY" : route.method().name()) + " " + route.pattern();
    }

    private static List<String> descritas(List<PublicRoutes.Route> routes) {
        return routes.stream().map(PublicRoutesTest::describe).toList();
    }

    @Nested
    @DisplayName("rutas de la cadena de negocio")
    class RutasDeNegocio {

        @Test
        @DisplayName("BUSINESS declara exactamente estas rutas publicas y ninguna mas")
        void business_declara_exactamente_estas_rutas_y_ninguna_mas() {
            assertThat(descritas(PublicRoutes.BUSINESS)).containsExactlyInAnyOrder(
                    "POST /auth/login/**", "POST /auth/refresh", "POST /register",
                    "POST /register/verify", "POST /auth/forgot-password",
                    "GET /auth/reset-password/validate", "POST /auth/reset-password",
                    "POST /auth/recover-code", "POST /dian/webhooks/**", "GET /countries",
                    "GET /countries/{countryId}/states", "GET /states/{stateId}/cities",
                    "GET /species/{specieId}/breeds", "GET /species", "GET /animal-colors",
                    "GET /consultation-types", "GET /modules", "GET /sub-modules", "GET /spa-types",
                    "GET /configurator/questionnaire", "POST /configurator/resolve", "GET /plans",
                    "GET /catalog", "POST /quotes/preview", "POST /platform/access-request",
                    "GET /platform/access-request/validate",
                    "POST /platform/access-request/approve", "POST /platform/access-request/reject",
                    "GET /platform/invitation/validate", "POST /platform/invitation/accept",
                    "ANY /swagger-ui/**", "ANY /v3/api-docs/**", "ANY /swagger-resources/**",
                    "ANY /webjars/**");
        }

        @Test
        @DisplayName("solo la documentacion OpenAPI se abre a cualquier metodo HTTP")
        void solo_la_documentacion_se_abre_a_cualquier_metodo() {
            List<String> sinMetodo = PublicRoutes.BUSINESS.stream()
                    .filter(PublicRoutes.Route::anyMethod).map(PublicRoutes.Route::pattern)
                    .toList();

            assertThat(sinMetodo).containsExactlyInAnyOrder("/swagger-ui/**", "/v3/api-docs/**",
                    "/swagger-resources/**", "/webjars/**");
        }
    }

    @Nested
    @DisplayName("rutas de otras cadenas")
    class RutasDeOtrasCadenas {

        @Test
        @DisplayName("OTHER_CHAINS es solo Actuator, que tiene su propia cadena y su propio basic")
        void other_chains_es_solo_actuator() {
            assertThat(descritas(PublicRoutes.OTHER_CHAINS)).containsExactly("ANY /actuator/**");
        }

        @Test
        @DisplayName("nada de OTHER_CHAINS se cuela en BUSINESS: alli seria permitAll de verdad")
        void nada_de_other_chains_se_cuela_en_business() {
            assertThat(descritas(PublicRoutes.BUSINESS))
                    .doesNotContainAnyElementsOf(descritas(PublicRoutes.OTHER_CHAINS));
        }
    }

    @Nested
    @DisplayName("lo que el AuthFilter deja pasar")
    class LoQueElFiltroDejaPasar {

        @Test
        @DisplayName("JWT_EXCLUDED es la union exacta de las dos listas, en ese orden")
        void jwt_excluded_es_la_union_exacta_de_las_dos_listas() {
            List<String> union = Stream.concat(descritas(PublicRoutes.BUSINESS).stream(),
                    descritas(PublicRoutes.OTHER_CHAINS).stream()).toList();

            assertThat(descritas(PublicRoutes.JWT_EXCLUDED)).containsExactlyElementsOf(union);
        }

        @Test
        @DisplayName("ninguna ruta esta declarada dos veces")
        void ninguna_ruta_esta_declarada_dos_veces() {
            assertThat(descritas(PublicRoutes.JWT_EXCLUDED)).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("ningun patron publico abre la raiz de la API")
        void ningun_patron_publico_abre_la_raiz_de_la_api() {
            List<String> patrones = PublicRoutes.JWT_EXCLUDED.stream()
                    .map(PublicRoutes.Route::pattern).toList();

            assertThat(patrones).doesNotContain("/**", "/*", "/")
                    .allSatisfy(patron -> assertThat(patron).startsWith("/"));
        }

        @Test
        @DisplayName("las listas son inmutables: nadie abre una ruta en caliente")
        void las_listas_son_inmutables() {
            assertThat(PublicRoutes.BUSINESS).isUnmodifiable();
            assertThat(PublicRoutes.OTHER_CHAINS).isUnmodifiable();
            assertThat(PublicRoutes.JWT_EXCLUDED).isUnmodifiable();
        }
    }

    @Nested
    @DisplayName("invariantes de Route")
    class InvariantesDeRoute {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("una ruta sin patron no llega a existir")
        void una_ruta_sin_patron_no_llega_a_existir(String patron) {
            assertThatThrownBy(() -> new PublicRoutes.Route(HttpMethod.GET, patron))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pattern is required");
        }

        @Test
        @DisplayName("anyMethod separa la ruta sin metodo de la que exige uno")
        void any_method_separa_la_ruta_sin_metodo_de_la_que_exige_uno() {
            assertThat(new PublicRoutes.Route(null, "/webjars/**").anyMethod()).isTrue();
            assertThat(new PublicRoutes.Route(HttpMethod.GET, "/countries").anyMethod()).isFalse();
        }
    }
}
