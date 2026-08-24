package com.vetsoftware.app.entitlement.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.entitlement.application.command.RecalculateCompanyEntitlementsCommand;
import com.vetsoftware.app.entitlement.application.dto.CompanyAccessDto;
import com.vetsoftware.app.entitlement.application.dto.CompanyCapacityDto;
import com.vetsoftware.app.entitlement.application.dto.CompanyEntitlementDto;
import com.vetsoftware.app.entitlement.application.dto.EntitlementRecalculationDto;
import com.vetsoftware.app.entitlement.application.dto.SubModuleSummaryDto;
import com.vetsoftware.app.entitlement.application.port.in.FindCompanyAccessUseCase;
import com.vetsoftware.app.entitlement.application.port.in.ListCompanyEntitlementsUseCase;
import com.vetsoftware.app.entitlement.application.port.in.RecalculateCompanyEntitlementsUseCase;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP: rutas, forma del JSON y --lo que de verdad importa aqui-- que la
 * empresa la ponga el servidor desde el principal y no el cliente.
 */
@WebMvcTest(CompanyEntitlementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CompanyEntitlementController — contrato HTTP")
class CompanyEntitlementControllerTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 15, 9, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindCompanyAccessUseCase findAccessUseCase;
    @MockitoBean
    private ListCompanyEntitlementsUseCase listUseCase;
    @MockitoBean
    private RecalculateCompanyEntitlementsUseCase recalculateUseCase;
    @MockitoBean
    private Authz authz;

    private static CompanyEntitlementDto permiso() {
        return new CompanyEntitlementDto(7L, 10L,
                new SubModuleSummaryDto(1L, "CLINICAL_HISTORY", "Historia clinica"), "READ_ONLY",
                "SUBSCRIPTION", 500L, 900L, AHORA.minusDays(30), null, AHORA);
    }

    @Test
    @DisplayName("GET /entitlements/access devuelve el acceso de la empresa del principal")
    void get_access_devuelve_el_acceso_de_la_empresa_del_principal() throws Exception {
        when(authz.currentCompanyId()).thenReturn(10L);
        when(findAccessUseCase.findByCompanyId(10L)).thenReturn(new CompanyAccessDto(10L,
                List.of(permiso()),
                List.of(new CompanyCapacityDto(31L, 10L, "USER", 3, 5, true, 500L, AHORA)), AHORA));

        mockMvc.perform(get("/entitlements/access")).andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(10))
                .andExpect(jsonPath("$.entitlements[0].subModule.code").value("CLINICAL_HISTORY"))
                .andExpect(jsonPath("$.entitlements[0].accessLevel").value("READ_ONLY"))
                .andExpect(jsonPath("$.capacities[0].exhausted").value(true));
    }

    @Test
    @DisplayName("GET /entitlements pagina con el contrato unico del proyecto")
    void get_entitlements_pagina_con_el_contrato_unico() throws Exception {
        when(authz.currentCompanyId()).thenReturn(10L);
        when(listUseCase.listByCompanyId(eq(10L), eq(1), eq(50)))
                .thenReturn(PageResult.of(List.of(permiso()), 1, 50, 60L));

        mockMvc.perform(get("/entitlements").param("page", "1").param("pageSize", "50"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(50))
                .andExpect(jsonPath("$.totalElements").value(60))
                .andExpect(jsonPath("$.content[0].id").value(7));
    }

    @Test
    @DisplayName("POST /entitlements/recalculate toma la empresa del principal, nunca del cliente")
    void post_recalculate_toma_la_empresa_del_principal() throws Exception {
        when(authz.currentCompanyId()).thenReturn(10L);
        when(recalculateUseCase.execute(any()))
                .thenReturn(new EntitlementRecalculationDto(10L, 500L, "ACTIVE", 12, 1, 2, AHORA));

        mockMvc.perform(post("/entitlements/recalculate")).andExpect(status().isOk())
                .andExpect(jsonPath("$.entitlementCount").value(12))
                .andExpect(jsonPath("$.manualGrantCount").value(1))
                .andExpect(jsonPath("$.contractStatus").value("ACTIVE"));

        ArgumentCaptor<RecalculateCompanyEntitlementsCommand> comando = ArgumentCaptor
                .forClass(RecalculateCompanyEntitlementsCommand.class);
        verify(recalculateUseCase).execute(comando.capture());
        org.assertj.core.api.Assertions.assertThat(comando.getValue().companyId()).isEqualTo(10L);
    }
}
