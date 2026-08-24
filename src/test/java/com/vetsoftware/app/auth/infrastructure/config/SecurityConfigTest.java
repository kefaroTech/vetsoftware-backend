package com.vetsoftware.app.auth.infrastructure.config;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.filter.AuthFilter;
import com.vetsoftware.app.auth.infrastructure.security.SecurityProblemDetailHandler;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

/**
 * Lo que se prueba aqui <b>es el cableado</b>, que es el unico caso en que este
 * repositorio admite {@code @SpringBootTest}: la cadena de negocio de
 * {@code SecurityConfig} solo existe montada, y sus tres decisiones —quien
 * entra sin token, con que verbo, y con que cabeceras sale la respuesta— viven
 * en metodos privados estaticos que no se pueden invocar de otro modo.
 *
 * <p>
 * El {@code AuthFilter} se sustituye por una subclase que nunca filtra. No es
 * por comodidad: ese filtro rechaza por su cuenta toda request sin
 * {@code Bearer}, asi que con el activo <em>cualquier</em> ruta privada daria
 * 401 y el 401 no probaria nada sobre esta clase. Neutralizado, quien responde
 * es el {@code anyRequest().authenticated()} de la cadena — la segunda barrera
 * que el javadoc de {@code SecurityConfig.authorize} dice cubrir «por si ese
 * filtro cambia, se reordena o se salta», y que hasta #126 nadie comprobaba.
 */
@SpringBootTest(classes = SecurityConfigTest.TestApplication.class, properties = {
        "management.health.redis.enabled=false"})
@AutoConfigureMockMvc
@DisplayName("SecurityConfig — la segunda barrera de autorizacion")
class SecurityConfigTest {

    /** El valor literal de {@code API_CONTENT_SECURITY_POLICY}, que es privado. */
    private static final String CSP_DE_LA_API = "default-src 'none'; frame-ancestors 'none'; "
            + "base-uri 'none'; form-action 'none'";

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("lo declarado en PublicRoutes entra sin token")
    class RutasPublicas {

        @Test
        @DisplayName("una ruta publica responde sin cabecera Authorization")
        void una_ruta_publica_responde_sin_cabecera_authorization() throws Exception {
            mockMvc.perform(get("/countries")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("el verbo importa: /register esta abierto en POST y cerrado en GET")
        void el_verbo_importa_en_una_ruta_publica() throws Exception {
            mockMvc.perform(post("/register")).andExpect(status().isOk());
            mockMvc.perform(get("/register")).andExpect(status().isUnauthorized());
        }

        /**
         * Las dos mitades del asistente de venta, juntas y en la misma prueba a
         * proposito: el defecto que arreglan era exactamente que una entraba y la otra
         * no. Ninguno de los dos {@code *ControllerTest} del configurador podia
         * atraparlo — los dos usan {@code addFilters = false}, asi que la cadena de
         * seguridad no se ejercitaba en esa feature.
         */
        @Test
        @DisplayName("las dos mitades del asistente de venta entran sin token: leer el cuestionario y resolverlo")
        void las_dos_mitades_del_asistente_entran_sin_token() throws Exception {
            mockMvc.perform(get("/configurator/questionnaire")).andExpect(status().isOk());
            mockMvc.perform(post("/configurator/resolve")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("abrir el asistente NO abre la administracion del cuestionario: el patron es exacto")
        void abrir_el_asistente_no_abre_la_administracion() throws Exception {
            mockMvc.perform(post("/configurator/questions")).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("TOKEN_MISSING"));
            mockMvc.perform(get("/configurator/questions")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("el verbo importa tambien aqui: /configurator/resolve esta abierto en POST y cerrado en GET")
        void el_verbo_importa_en_configurator_resolve() throws Exception {
            mockMvc.perform(post("/configurator/resolve")).andExpect(status().isOk());
            mockMvc.perform(get("/configurator/resolve")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("la documentacion OpenAPI se abre a cualquier metodo HTTP")
        void la_documentacion_openapi_se_abre_a_cualquier_metodo() throws Exception {
            mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
            mockMvc.perform(post("/swagger-ui/index.html")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("todo lo demas exige identidad")
    class RestoAutenticado {

        @Test
        @DisplayName("una ruta de negocio sin principal responde 401 con code TOKEN_MISSING")
        void una_ruta_de_negocio_sin_principal_responde_401() throws Exception {
            mockMvc.perform(get("/animals")).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("TOKEN_MISSING"));
        }

        @Test
        @DisplayName("el preflight CORS no lleva Authorization y no se rechaza")
        void el_preflight_cors_no_se_rechaza() throws Exception {
            mockMvc.perform(options("/animals")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("cabeceras de seguridad")
    class CabecerasDeSeguridad {

        @Test
        @DisplayName("la respuesta lleva el juego completo de cabeceras defensivas")
        void la_respuesta_lleva_el_juego_completo_de_cabeceras_defensivas() throws Exception {
            mockMvc.perform(get("/countries").secure(true)).andExpect(status().isOk())
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(
                            header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                    .andExpect(header().string("Strict-Transport-Security",
                            containsString("max-age=31536000")))
                    .andExpect(header().string("Strict-Transport-Security",
                            containsString("includeSubDomains")))
                    .andExpect(header().string("Permissions-Policy",
                            containsString("geolocation=()")));
        }

        @Test
        @DisplayName("la API declara una CSP que no deja cargar nada")
        void la_api_declara_una_csp_que_no_deja_cargar_nada() throws Exception {
            mockMvc.perform(get("/countries"))
                    .andExpect(header().string("Content-Security-Policy", CSP_DE_LA_API));
        }

        @Test
        @DisplayName("Swagger UI queda fuera de esa CSP o la consola no renderiza")
        void swagger_ui_queda_fuera_de_esa_csp() throws Exception {
            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(header().doesNotExist("Content-Security-Policy"));
        }
    }

    /**
     * Endpoints de juguete sobre los patrones reales de {@code PublicRoutes}. Sin
     * declarar metodo a proposito: asi el controller acepta cualquier verbo y quien
     * decide si la request pasa es exclusivamente la cadena de filtros.
     */
    @RestController
    @TestComponent
    static class ProbeController {

        @RequestMapping({"/countries", "/register", "/animals", "/swagger-ui/index.html",
                "/configurator/questionnaire", "/configurator/resolve", "/configurator/questions"})
        String probe() {
            return "ok";
        }
    }

    /**
     * Raiz de contexto de este test, nombrada en el
     * {@code @SpringBootTest(classes = ...)} y marcada con {@code @TestComponent}
     * por {@code PiramideDeTestsTest.DOBLE_DE_TEST_NO_ESCANEABLE}.
     */
    @SpringBootConfiguration
    @TestComponent
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class, DataJpaRepositoriesAutoConfiguration.class,
            LiquibaseAutoConfiguration.class})
    @Import({SecurityAutoConfiguration.class, SecurityConfig.class, ProbeController.class})
    static class TestApplication {

        /**
         * Subclase que nunca filtra (ver el javadoc de la clase). De los siete
         * colaboradores del constructor solo se desreferencia el {@code MeterRegistry},
         * que construye el contador de rechazos; el resto no se toca porque
         * {@code doFilterInternal} no llega a ejecutarse nunca.
         */
        @Bean
        AuthFilter authFilter() {
            return new AuthFilter(null, null, null, null, null, null, new SimpleMeterRegistry()) {
                @Override
                protected boolean shouldNotFilter(HttpServletRequest request) {
                    return true;
                }
            };
        }

        /**
         * El handler real, no un doble: es quien convierte el rechazo de la cadena en
         * el {@code ProblemDetail} con {@code code} que afirma este test. Sus dos
         * colaboradores de telemetria si son dobles — no participan en la decision, y
         * {@code tracer.currentSpan()} devolviendo {@code null} es un camino que el
         * propio handler contempla.
         *
         * <p>
         * El {@code ObjectMapper} <b>tiene que ser el del contenedor</b>, que es el que
         * recibe el handler en produccion. Uno recien construido con {@code new} no
         * lleva el mixin de {@code ProblemDetail}, asi que anida las propiedades
         * —{@code "properties":{"code":"TOKEN_MISSING"}}— en vez de ponerlas al mismo
         * nivel que {@code status} y {@code detail}. Con ese mapper el cuerpo sale
         * distinto del que ve el front, y el test afirmaria sobre una forma que en
         * produccion no existe.
         */
        @Bean
        SecurityProblemDetailHandler securityProblemDetailHandler(ObjectMapper objectMapper) {
            return new SecurityProblemDetailHandler(objectMapper, mock(AuditLogger.class),
                    mock(Tracer.class));
        }
    }
}
