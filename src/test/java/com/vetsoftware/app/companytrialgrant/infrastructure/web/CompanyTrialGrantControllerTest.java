package com.vetsoftware.app.companytrialgrant.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.companytrialgrant.application.dto.CompanyTrialGrantDto;
import com.vetsoftware.app.companytrialgrant.application.port.in.ListCompanyTrialGrantsUseCase;
import com.vetsoftware.app.companytrialgrant.domain.TrialPolicyOutcome;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.lang.reflect.Method;
import java.time.LocalDate;
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
 * Rodaja HTTP de lo que la clínica ve de sus propias pruebas.
 *
 * <p>
 * Además del reparto habitual —la empresa la pone el servidor— aquí se clava
 * una regla propia del bloque: <strong>la superficie del tenant no ofrece
 * ninguna forma de desconceder</strong> (R-TRIAL-22).
 */
@WebMvcTest(CompanyTrialGrantController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CompanyTrialGrantController — contrato HTTP")
class CompanyTrialGrantControllerTest {

    private static final Long MI_EMPRESA = 9L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListCompanyTrialGrantsUseCase listUseCase;
    @MockitoBean
    private Authz authz;

    /**
     * Concedida el día 16 de una ventana que acaba el 30: quince días concedidos
     * por la oferta, quince efectivos. La política del artículo permitía treinta.
     */
    private static CompanyTrialGrantDto concesionDe(Long companyId) {
        return new CompanyTrialGrantDto(11L, companyId, 5L, 3L, LocalDate.of(2026, 9, 16), 15, 15,
                LocalDate.of(2026, 9, 30), 30, TrialPolicyOutcome.LIMITED, 77L, null, null, null,
                true);
    }

    @Nested
    @DisplayName("Lectura del tenant")
    class LecturaDelTenant {

        @Test
        @DisplayName("GET devuelve las concesiones de la clinica")
        void get_devuelve_las_concesiones() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(listUseCase.listByCompanyId(MI_EMPRESA))
                    .thenReturn(List.of(concesionDe(MI_EMPRESA)));

            mockMvc.perform(get("/company-trial-grants")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].companyId").value(9))
                    .andExpect(jsonPath("$[0].catalogItemId").value(5))
                    .andExpect(jsonPath("$[0].policyTrialOutcome").value("LIMITED"))
                    .andExpect(jsonPath("$[0].live").value(true));
        }

        /**
         * <strong>Los días efectivos viajan, no se deducen.</strong> Un módulo añadido
         * el día 16 de una ventana que acaba el 30 recibe 15 días, no los 30 de la
         * política: enseñar el número de la política sería prometerle al cliente días
         * que no va a tener.
         */
        @Test
        @DisplayName("la respuesta lleva los dias efectivos ademas de los de la politica")
        void la_respuesta_lleva_los_dias_efectivos() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(listUseCase.listByCompanyId(MI_EMPRESA))
                    .thenReturn(List.of(concesionDe(MI_EMPRESA)));

            mockMvc.perform(get("/company-trial-grants")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].daysGranted").value(15))
                    .andExpect(jsonPath("$[0].effectiveDays").value(15))
                    .andExpect(jsonPath("$[0].policyTrialDays").value(30))
                    .andExpect(jsonPath("$[0].trialEndDate").value("2026-09-30"));
        }

        @Test
        @DisplayName("una clinica que nunca probo nada recibe una lista vacia, no un 404")
        void sin_concesiones_lista_vacia() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(listUseCase.listByCompanyId(MI_EMPRESA)).thenReturn(List.of());

            mockMvc.perform(get("/company-trial-grants")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la empresa la pone el servidor: un parametro companyId en la URL se ignora")
        void la_empresa_la_pone_el_servidor() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(listUseCase.listByCompanyId(MI_EMPRESA)).thenReturn(List.of());

            mockMvc.perform(get("/company-trial-grants").param("companyId", "77"))
                    .andExpect(status().isOk());

            verify(listUseCase).listByCompanyId(MI_EMPRESA);
        }

        @Test
        @DisplayName("un principal sin empresa responde 403 y no llega al caso de uso")
        void principal_sin_empresa_responde_403() throws Exception {
            when(authz.currentCompanyId())
                    .thenThrow(new AccessDeniedException("No employee context"));

            mockMvc.perform(get("/company-trial-grants")).andExpect(status().isForbidden());

            verifyNoInteractions(listUseCase);
        }
    }

    @Nested
    @DisplayName("Autorizacion")
    class Autorizacion {

        @Test
        @DisplayName("el puerto admite SYSTEM o la propia empresa, y revalida el tenant")
        void el_puerto_admite_system_o_la_propia_empresa() throws Exception {
            String gate = ListCompanyTrialGrantsUseCase.class
                    .getMethod("listByCompanyId", Long.class).getAnnotation(PreAuthorize.class)
                    .value();

            assertThat(gate).contains("hasRole('SYSTEM')")
                    .contains("@authz.isMyCompany(#companyId)");
        }

        @Test
        @DisplayName("el parametro del puerto se llama companyId, que es lo que nombra el SpEL")
        void el_parametro_se_llama_company_id() throws Exception {
            assertThat(ListCompanyTrialGrantsUseCase.class.getMethod("listByCompanyId", Long.class)
                    .getParameters()[0].getName()).isEqualTo("companyId");
        }

        @Test
        @DisplayName("la ruta del controller del tenant no lleva companyId")
        void la_ruta_no_lleva_company_id() {
            assertThat(
                    CompanyTrialGrantController.class.getAnnotation(RequestMapping.class).value())
                    .containsExactly("/company-trial-grants");
        }
    }

    @Nested
    @DisplayName("Una concesion no se desconcede")
    class UnaConcesionNoSeDesconcede {

        /**
         * {@code TrialGrantWriteSurfaceTest} cierra el agregado, los puertos y el
         * repositorio. Esto cierra la cuarta superficie —la única que un cliente
         * alcanza de verdad— y por el nombre del método, que es lo que decide la ruta
         * que se publica.
         */
        @Test
        @DisplayName("el controller no publica ningun metodo que borre o desactive")
        void el_controller_no_publica_ningun_borrado() {
            assertThat(Arrays.stream(CompanyTrialGrantController.class.getDeclaredMethods())
                    .map(Method::getName))
                    .noneMatch(nombre -> nombre.startsWith("delete") || nombre.startsWith("remove")
                            || nombre.startsWith("revoke") || nombre.startsWith("disable"));
        }

        @Test
        @DisplayName("no existe ningun DELETE sobre las concesiones del tenant")
        void no_existe_delete() throws Exception {
            mockMvc.perform(delete("/company-trial-grants")).andExpect(status().is4xxClientError());
        }
    }
}
