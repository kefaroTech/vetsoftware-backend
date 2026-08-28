package com.vetsoftware.app.companylimitevent.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.companylimitevent.application.dto.CompanyLimitEventDto;
import com.vetsoftware.app.companylimitevent.application.port.in.ListCompanyLimitEventsUseCase;
import com.vetsoftware.app.companylimitevent.domain.LimitEventType;
import com.vetsoftware.app.companylimitevent.domain.LimitSource;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Rodaja HTTP de <strong>lo que el cliente ve de sus propios portazos</strong>.
 *
 * <p>
 * Es la mitad del valor de D-59 y lo que la ficha de construcción reparte al
 * bloque de contadores. La otra mitad —que el cliente <em>no</em> pueda
 * escribir en la bitácora ni corregirse el consumo— también se clava aquí, y
 * por la superficie: este controlador no publica una sola escritura.
 */
@WebMvcTest(CompanyLimitEventController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CompanyLimitEventController — contrato HTTP")
class CompanyLimitEventControllerTest {

    private static final Long MI_EMPRESA = 9L;
    private static final LocalDateTime DESDE = LocalDateTime.of(2026, 3, 1, 0, 0);
    private static final LocalDateTime HASTA = LocalDateTime.of(2026, 3, 31, 23, 59);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListCompanyLimitEventsUseCase listUseCase;
    @MockitoBean
    private Authz authz;

    /** Un portazo: se le negó crear la mascota 101 con el techo en 100. */
    private static CompanyLimitEventDto portazoDe(Long companyId) {
        return new CompanyLimitEventDto(88L, companyId, 4L, LimitEventType.LIMIT_BLOCKED, 100, 100,
                1, LimitSource.SUBSCRIPTION, null, null, null, true, null, null,
                LocalDateTime.of(2026, 3, 14, 11, 5));
    }

    @Nested
    @DisplayName("Lectura del tenant")
    class LecturaDelTenant {

        @Test
        @DisplayName("GET devuelve los hechos de cupo de la clinica en el rango")
        void get_devuelve_los_hechos() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(listUseCase.listByCompanyId(MI_EMPRESA, DESDE, HASTA))
                    .thenReturn(List.of(portazoDe(MI_EMPRESA)));

            mockMvc.perform(get("/company-limit-events").param("from", "2026-03-01T00:00:00")
                    .param("to", "2026-03-31T23:59:00")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].companyId").value(9))
                    .andExpect(jsonPath("$[0].eventType").value("LIMIT_BLOCKED"))
                    .andExpect(jsonPath("$[0].limitQuantity").value(100))
                    .andExpect(jsonPath("$[0].requestedDelta").value(1));
        }

        /**
         * <strong>El origen del techo viaja como texto, no como tipo con nombre
         * propio.</strong> Hay dos {@code LimitSource} en el árbol y springdoc funde
         * por nombre simple: si esto volviera a ser un enumerado, el contrato tendría
         * un esquema compartido que hoy cuadra por casualidad.
         */
        @Test
        @DisplayName("el origen del techo sale como texto de la lista cerrada")
        void el_origen_sale_como_texto() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(listUseCase.listByCompanyId(MI_EMPRESA, DESDE, HASTA))
                    .thenReturn(List.of(portazoDe(MI_EMPRESA)));

            mockMvc.perform(get("/company-limit-events").param("from", "2026-03-01T00:00:00")
                    .param("to", "2026-03-31T23:59:00")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].limitSource").value("SUBSCRIPTION"))
                    .andExpect(jsonPath("$[0].actorIsProcess").value(true));
        }

        @Test
        @DisplayName("un mes sin portazos devuelve una lista vacia, no un 404")
        void mes_sin_portazos_lista_vacia() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(listUseCase.listByCompanyId(MI_EMPRESA, DESDE, HASTA)).thenReturn(List.of());

            mockMvc.perform(get("/company-limit-events").param("from", "2026-03-01T00:00:00")
                    .param("to", "2026-03-31T23:59:00")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        /**
         * El rango es obligatorio a propósito: la bitácora crece sin techo y un listado
         * sin ventana temporal acaba siendo un volcado de la tabla.
         */
        @Test
        @DisplayName("GET sin rango responde 400: la bitacora no se vuelca entera")
        void sin_rango_responde_400() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);

            mockMvc.perform(get("/company-limit-events")).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la empresa la pone el servidor: un parametro companyId en la URL se ignora")
        void la_empresa_la_pone_el_servidor() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(listUseCase.listByCompanyId(MI_EMPRESA, DESDE, HASTA)).thenReturn(List.of());

            mockMvc.perform(get("/company-limit-events").param("from", "2026-03-01T00:00:00")
                    .param("to", "2026-03-31T23:59:00").param("companyId", "77"))
                    .andExpect(status().isOk());

            verify(listUseCase).listByCompanyId(MI_EMPRESA, DESDE, HASTA);
        }

        @Test
        @DisplayName("un principal sin empresa responde 403 y no llega al caso de uso")
        void principal_sin_empresa_responde_403() throws Exception {
            when(authz.currentCompanyId())
                    .thenThrow(new AccessDeniedException("No employee context"));

            mockMvc.perform(get("/company-limit-events").param("from", "2026-03-01T00:00:00")
                    .param("to", "2026-03-31T23:59:00")).andExpect(status().isForbidden());

            verifyNoInteractions(listUseCase);
        }
    }

    @Nested
    @DisplayName("Autorizacion")
    class Autorizacion {

        @Test
        @DisplayName("el puerto admite SYSTEM o la propia empresa, y revalida el tenant")
        void el_puerto_admite_system_o_la_propia_empresa() throws Exception {
            String gate = ListCompanyLimitEventsUseCase.class.getMethod("listByCompanyId",
                    Long.class, LocalDateTime.class, LocalDateTime.class)
                    .getAnnotation(PreAuthorize.class).value();

            assertThat(gate).contains("hasRole('SYSTEM')")
                    .contains("@authz.isMyCompany(#companyId)");
        }

        @Test
        @DisplayName("el primer parametro del puerto se llama companyId, como nombra el SpEL")
        void el_parametro_se_llama_company_id() throws Exception {
            assertThat(ListCompanyLimitEventsUseCase.class.getMethod("listByCompanyId", Long.class,
                    LocalDateTime.class, LocalDateTime.class).getParameters()[0].getName())
                    .isEqualTo("companyId");
        }

        @Test
        @DisplayName("la ruta del controller del tenant no lleva companyId")
        void la_ruta_no_lleva_company_id() {
            assertThat(
                    CompanyLimitEventController.class.getAnnotation(RequestMapping.class).value())
                    .containsExactly("/company-limit-events");
        }
    }

    @Nested
    @DisplayName("El tenant no escribe en la bitacora")
    class ElTenantNoEscribe {

        /**
         * <strong>Ni el hecho de cupo ni la corrección del consumo se publican
         * aquí.</strong> El gate de {@code RecordLimitEventUseCase} admite al tenant
         * —tiene que hacerlo: el portazo nace dentro de una petición de la clínica—
         * pero su llamador es {@code LimitDenialAdapter}, no un cliente HTTP. Un
         * endpoint dejaría a la clínica fabricar portazos que nunca ocurrieron, y la
         * bitácora vale lo que vale su credibilidad.
         */
        @Test
        @DisplayName("el controller del tenant solo publica lecturas")
        void el_controller_solo_publica_lecturas() {
            assertThat(Arrays.stream(CompanyLimitEventController.class.getDeclaredMethods())
                    .map(Method::getName))
                    .noneMatch(nombre -> nombre.startsWith("record") || nombre.startsWith("adjust")
                            || nombre.startsWith("reconcile") || nombre.startsWith("create"));
        }

        @Test
        @DisplayName("no hay POST de tenant sobre la bitacora ni sobre el consumo")
        void no_hay_post_de_tenant() throws Exception {
            mockMvc.perform(post("/company-limit-events")).andExpect(status().is4xxClientError());
            mockMvc.perform(post("/company-limit-events/usage-adjustments"))
                    .andExpect(status().is4xxClientError());
        }
    }
}
