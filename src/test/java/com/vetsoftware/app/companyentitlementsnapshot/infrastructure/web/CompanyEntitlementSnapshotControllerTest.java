package com.vetsoftware.app.companyentitlementsnapshot.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.companyentitlementsnapshot.application.dto.CompanyEntitlementSnapshotDto;
import com.vetsoftware.app.companyentitlementsnapshot.application.port.in.FindEntitlementSnapshotAsOfUseCase;
import com.vetsoftware.app.companyentitlementsnapshot.domain.CompanyEntitlementSnapshotNotFoundException;
import com.vetsoftware.app.companyentitlementsnapshot.domain.SnapshotTriggerReason;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
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
 * Rodaja HTTP de «qué veía mi clínica el 3 de marzo».
 *
 * <p>
 * Es la pregunta entera por la que existe la tabla, y la superficie que la
 * responde tiene dos mitades igual de importantes: que el cliente pueda
 * <strong>leer</strong> su foto, y que no pueda <strong>escribirla</strong>.
 */
@WebMvcTest(CompanyEntitlementSnapshotController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CompanyEntitlementSnapshotController — contrato HTTP")
class CompanyEntitlementSnapshotControllerTest {

    private static final Long MI_EMPRESA = 9L;
    private static final LocalDateTime EL_3_DE_MARZO = LocalDateTime.of(2026, 3, 3, 12, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindEntitlementSnapshotAsOfUseCase findUseCase;
    @MockitoBean
    private Authz authz;

    private static CompanyEntitlementSnapshotDto fotoDe(Long companyId) {
        return new CompanyEntitlementSnapshotDto(70L, companyId,
                LocalDateTime.of(2026, 3, 1, 6, 30), null, null, true,
                SnapshotTriggerReason.TRIAL_EXPIRED, null, "{\"modules\":[\"CORE\"]}", 1);
    }

    @Nested
    @DisplayName("Lectura del tenant")
    class LecturaDelTenant {

        @Test
        @DisplayName("GET devuelve la ultima foto anterior o igual al instante pedido")
        void get_devuelve_la_ultima_foto() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(findUseCase.findLatestAsOf(MI_EMPRESA, EL_3_DE_MARZO))
                    .thenReturn(fotoDe(MI_EMPRESA));

            mockMvc.perform(
                    get("/company-entitlement-snapshots").param("at", "2026-03-03T12:00:00"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.companyId").value(9))
                    .andExpect(jsonPath("$.triggerReason").value("TRIAL_EXPIRED"))
                    .andExpect(jsonPath("$.actorIsProcess").value(true))
                    .andExpect(jsonPath("$.recalculatedAt").value("2026-03-01T06:30:00"));
        }

        /**
         * <strong>El documento sale tal cual, con su versión de formato al
         * lado.</strong> Sin la versión, una foto vieja es un blob que nadie sabe
         * interpretar: eso es la diferencia entre una prueba y un texto.
         */
        @Test
        @DisplayName("el documento viaja como cadena, con su version de formato")
        void el_documento_viaja_con_su_version() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(findUseCase.findLatestAsOf(MI_EMPRESA, EL_3_DE_MARZO))
                    .thenReturn(fotoDe(MI_EMPRESA));

            mockMvc.perform(
                    get("/company-entitlement-snapshots").param("at", "2026-03-03T12:00:00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload").value("{\"modules\":[\"CORE\"]}"))
                    .andExpect(jsonPath("$.payloadFormatVersion").value(1));
        }

        @Test
        @DisplayName("una empresa sin foto a esa fecha recibe 404, no 500")
        void sin_foto_responde_404() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(findUseCase.findLatestAsOf(MI_EMPRESA, EL_3_DE_MARZO)).thenThrow(
                    new CompanyEntitlementSnapshotNotFoundException(MI_EMPRESA, EL_3_DE_MARZO));

            mockMvc.perform(
                    get("/company-entitlement-snapshots").param("at", "2026-03-03T12:00:00"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET sin instante responde 400: la foto es siempre a una fecha")
        void sin_instante_responde_400() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);

            mockMvc.perform(get("/company-entitlement-snapshots"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la empresa la pone el servidor: un parametro companyId en la URL se ignora")
        void la_empresa_la_pone_el_servidor() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(findUseCase.findLatestAsOf(MI_EMPRESA, EL_3_DE_MARZO))
                    .thenReturn(fotoDe(MI_EMPRESA));

            mockMvc.perform(get("/company-entitlement-snapshots").param("at", "2026-03-03T12:00:00")
                    .param("companyId", "77")).andExpect(status().isOk());

            verify(findUseCase).findLatestAsOf(MI_EMPRESA, EL_3_DE_MARZO);
        }

        @Test
        @DisplayName("un principal sin empresa responde 403 y no llega al caso de uso")
        void principal_sin_empresa_responde_403() throws Exception {
            when(authz.currentCompanyId())
                    .thenThrow(new AccessDeniedException("No employee context"));

            mockMvc.perform(
                    get("/company-entitlement-snapshots").param("at", "2026-03-03T12:00:00"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(findUseCase);
        }
    }

    @Nested
    @DisplayName("Autorizacion")
    class Autorizacion {

        @Test
        @DisplayName("el puerto admite SYSTEM o la propia empresa, y revalida el tenant")
        void el_puerto_admite_system_o_la_propia_empresa() throws Exception {
            String gate = FindEntitlementSnapshotAsOfUseCase.class
                    .getMethod("findLatestAsOf", Long.class, LocalDateTime.class)
                    .getAnnotation(PreAuthorize.class).value();

            assertThat(gate).contains("hasRole('SYSTEM')")
                    .contains("@authz.isMyCompany(#companyId)");
        }

        @Test
        @DisplayName("el primer parametro del puerto se llama companyId, como nombra el SpEL")
        void el_parametro_se_llama_company_id() throws Exception {
            assertThat(FindEntitlementSnapshotAsOfUseCase.class
                    .getMethod("findLatestAsOf", Long.class, LocalDateTime.class).getParameters()[0]
                    .getName()).isEqualTo("companyId");
        }

        @Test
        @DisplayName("la ruta del controller del tenant no lleva companyId")
        void la_ruta_no_lleva_company_id() {
            assertThat(CompanyEntitlementSnapshotController.class
                    .getAnnotation(RequestMapping.class).value())
                    .containsExactly("/company-entitlement-snapshots");
        }
    }

    @Nested
    @DisplayName("La foto no se fabrica desde fuera")
    class LaFotoNoSeFabricaDesdeFuera {

        /**
         * <strong>Guardar una foto no tiene endpoint, y esa ausencia es la
         * regla.</strong> El gate de {@code RecordEntitlementSnapshotUseCase} admite al
         * tenant porque el recálculo se dispara desde la propia clínica, pero su
         * llamador es {@code CompanyEntitlementSnapshotAdapter}, dentro de la misma
         * transacción del recálculo. Un endpoint que aceptara el {@code payload} de
         * fuera dejaría que quien reclama escriba él mismo la prueba con la que
         * reclama.
         */
        @Test
        @DisplayName("el controller solo publica lecturas: ninguna escribe la foto")
        void el_controller_solo_publica_lecturas() {
            assertThat(
                    Arrays.stream(CompanyEntitlementSnapshotController.class.getDeclaredMethods())
                            .map(Method::getName))
                    .noneMatch(nombre -> nombre.startsWith("record") || nombre.startsWith("create")
                            || nombre.startsWith("save"));
        }

        @Test
        @DisplayName("no hay POST que escriba una foto desde el tenant")
        void no_hay_post_de_tenant() throws Exception {
            mockMvc.perform(post("/company-entitlement-snapshots"))
                    .andExpect(status().is4xxClientError());
        }
    }
}
