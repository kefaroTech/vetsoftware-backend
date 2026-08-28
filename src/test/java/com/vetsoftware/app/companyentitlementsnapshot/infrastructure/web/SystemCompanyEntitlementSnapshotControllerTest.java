package com.vetsoftware.app.companyentitlementsnapshot.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP de la misma foto vista desde plataforma.
 *
 * <p>
 * <strong>Existe porque quien atiende la reclamación no es la clínica.</strong>
 * Si la única lectura fuera la del tenant, contestar «qué permisos tenías el 3
 * de marzo» exigiría pedirle al cliente que lo mirara él, que es justo lo
 * contrario de una prueba. La empresa entra por la ruta porque un usuario de
 * plataforma no tiene empresa que derivar.
 */
@WebMvcTest(SystemCompanyEntitlementSnapshotController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemCompanyEntitlementSnapshotController — contrato HTTP")
class SystemCompanyEntitlementSnapshotControllerTest {

    private static final Long LA_CLINICA = 42L;
    private static final LocalDateTime EL_3_DE_MARZO = LocalDateTime.of(2026, 3, 3, 12, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindEntitlementSnapshotAsOfUseCase findUseCase;

    private static CompanyEntitlementSnapshotDto foto() {
        return new CompanyEntitlementSnapshotDto(70L, LA_CLINICA,
                LocalDateTime.of(2026, 3, 1, 6, 30), null, WebMvcSliceConfig.SYSTEM_USER_ID, false,
                SnapshotTriggerReason.CONTRACT_AMENDMENT, 12L, "{\"modules\":[\"CORE\"]}", 1);
    }

    @Nested
    @DisplayName("Lectura de plataforma")
    class LecturaDePlataforma {

        @Test
        @DisplayName("GET /companies/{id} lee la foto de la empresa que pide la ruta")
        void get_lee_la_empresa_de_la_ruta() throws Exception {
            when(findUseCase.findLatestAsOf(LA_CLINICA, EL_3_DE_MARZO)).thenReturn(foto());

            mockMvc.perform(get("/system/company-entitlement-snapshots/companies/42").param("at",
                    "2026-03-03T12:00:00")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.companyId").value(42))
                    .andExpect(jsonPath("$.triggerReason").value("CONTRACT_AMENDMENT"))
                    .andExpect(jsonPath("$.amendmentId").value(12));

            verify(findUseCase).findLatestAsOf(LA_CLINICA, EL_3_DE_MARZO);
        }

        /**
         * El actor sale desplegado: exactamente uno de los tres está relleno, y el
         * motor lo impone. Aquí firmó una persona de plataforma, no el proceso.
         */
        @Test
        @DisplayName("el actor viaja desplegado y solo uno de los tres esta relleno")
        void el_actor_viaja_desplegado() throws Exception {
            when(findUseCase.findLatestAsOf(LA_CLINICA, EL_3_DE_MARZO)).thenReturn(foto());

            mockMvc.perform(get("/system/company-entitlement-snapshots/companies/42").param("at",
                    "2026-03-03T12:00:00")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.actorEmployeeId").doesNotExist())
                    .andExpect(jsonPath("$.actorSystemUserId").value(6))
                    .andExpect(jsonPath("$.actorIsProcess").value(false));
        }

        @Test
        @DisplayName("una empresa sin foto a esa fecha recibe 404, no 500")
        void sin_foto_responde_404() throws Exception {
            when(findUseCase.findLatestAsOf(LA_CLINICA, EL_3_DE_MARZO)).thenThrow(
                    new CompanyEntitlementSnapshotNotFoundException(LA_CLINICA, EL_3_DE_MARZO));

            mockMvc.perform(get("/system/company-entitlement-snapshots/companies/42").param("at",
                    "2026-03-03T12:00:00")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET sin instante responde 400: la foto es siempre a una fecha")
        void sin_instante_responde_400() throws Exception {
            mockMvc.perform(get("/system/company-entitlement-snapshots/companies/42"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("La foto no se fabrica desde fuera")
    class LaFotoNoSeFabricaDesdeFuera {

        /**
         * El motivo no cambia por el actor: la foto es consecuencia de un recálculo, y
         * un endpoint que aceptara el {@code payload} de fuera convertiría la bitácora
         * probatoria en un formulario — también para plataforma.
         */
        @Test
        @DisplayName("el controller de plataforma tampoco publica ninguna escritura")
        void el_controller_no_publica_escrituras() {
            assertThat(Arrays
                    .stream(SystemCompanyEntitlementSnapshotController.class.getDeclaredMethods())
                    .map(Method::getName))
                    .noneMatch(nombre -> nombre.startsWith("record") || nombre.startsWith("create")
                            || nombre.startsWith("save"));
        }

        @Test
        @DisplayName("no hay POST que escriba una foto desde plataforma")
        void no_hay_post_de_plataforma() throws Exception {
            mockMvc.perform(post("/system/company-entitlement-snapshots/companies/42"))
                    .andExpect(status().is4xxClientError());
        }
    }
}
