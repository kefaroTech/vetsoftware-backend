package com.vetsoftware.app.subscriptionmodule.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.subscriptionmodule.application.command.ListModuleShowcaseQuery;
import com.vetsoftware.app.subscriptionmodule.application.dto.ModuleCeilingDto;
import com.vetsoftware.app.subscriptionmodule.application.dto.ModuleShowcaseDto;
import com.vetsoftware.app.subscriptionmodule.application.port.in.ListModuleShowcaseUseCase;
import com.vetsoftware.app.subscriptionmodule.domain.ModuleState;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
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

@WebMvcTest(SubscriptionModuleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SubscriptionModuleController — contrato HTTP")
class SubscriptionModuleControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Authz authz;
    @MockitoBean
    private ListModuleShowcaseUseCase listUseCase;

    @Test
    @DisplayName("GET /subscriptions/modules deriva companyId y employeeId del contexto, nunca de la petición")
    void deriva_companyId_y_employeeId_del_contexto() throws Exception {
        Long employeeId = 55L;
        when(authz.currentEmployeeId()).thenReturn(employeeId);
        when(listUseCase.execute(any())).thenReturn(List.of(new ModuleShowcaseDto("GROOMING",
                "Spa y guardería", "Servicios de estética", ModuleState.TRIAL,
                LocalDate.of(2026, 9, 30),
                List.of(new ModuleCeilingDto("GROOMING_SERVICE", "FLOW", 5, 30, 60, "BLOCK")),
                new BigDecimal("59900"), new BigDecimal("599000"), true, true)));

        mockMvc.perform(get("/subscriptions/modules")).andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$[0].code").value("GROOMING"))
                .andExpect(jsonPath("$[0].state").value("TRIAL"))
                .andExpect(jsonPath("$[0].ceilings[0].dimensionCode").value("GROOMING_SERVICE"))
                .andExpect(jsonPath("$[0].canPurchase").value(true));

        ArgumentCaptor<ListModuleShowcaseQuery> queryCaptor = ArgumentCaptor
                .forClass(ListModuleShowcaseQuery.class);
        verify(listUseCase).execute(queryCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(queryCaptor.getValue().companyId())
                .isEqualTo(WebMvcSliceConfig.COMPANY_ID);
        org.assertj.core.api.Assertions.assertThat(queryCaptor.getValue().employeeId())
                .isEqualTo(employeeId);
    }
}
