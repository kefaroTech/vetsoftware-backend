package com.vetsoftware.app.dunning.infrastructure.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.dunning.application.port.in.FindDunningEventUseCase;
import com.vetsoftware.app.dunning.application.port.in.ListDunningEventsBySubscriptionUseCase;
import com.vetsoftware.app.dunning.application.port.in.RecordDunningEventUseCase;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DunningEventController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("DunningEventController — contrato HTTP")
class DunningEventControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RecordDunningEventUseCase recordUseCase;
    @MockitoBean
    private FindDunningEventUseCase findUseCase;
    @MockitoBean
    private ListDunningEventsBySubscriptionUseCase listUseCase;
    @MockitoBean
    private Authz authz;

    @Test
    @DisplayName("acota el expediente de cobranza a la empresa autenticada")
    void acota_el_expediente_de_cobranza_a_la_empresa_autenticada() throws Exception {
        when(authz.currentCompanyId()).thenReturn(77L);
        when(listUseCase.listBySubscription(31L, 77L, 0, 20)).thenReturn(PageResult.empty(0, 20));

        mockMvc.perform(get("/dunning-events").param("subscriptionId", "31"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(listUseCase).listBySubscription(31L, 77L, 0, 20);
    }
}
