package com.vetsoftware.app.subscriptionbilling.infrastructure.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionbilling.application.port.in.FindBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.FindSubscriptionChargeUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.ListBillingDocumentsUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.ListSubscriptionChargesUseCase;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SubscriptionBillingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SubscriptionBillingController — contrato HTTP")
class SubscriptionBillingControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ListSubscriptionChargesUseCase listChargesUseCase;
    @MockitoBean
    private FindSubscriptionChargeUseCase findChargeUseCase;
    @MockitoBean
    private ListBillingDocumentsUseCase listDocumentsUseCase;
    @MockitoBean
    private FindBillingDocumentUseCase findDocumentUseCase;
    @MockitoBean
    private Authz authz;

    @Test
    @DisplayName("lista documentos únicamente para la empresa del principal")
    void lista_documentos_unicamente_para_la_empresa_del_principal() throws Exception {
        when(authz.currentCompanyId()).thenReturn(77L);
        when(listDocumentsUseCase.listByCompany(77L, 1, 6)).thenReturn(PageResult.empty(1, 6));

        mockMvc.perform(
                get("/subscription-billing/documents").param("page", "1").param("pageSize", "6"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.pageSize").value(6));

        verify(listDocumentsUseCase).listByCompany(77L, 1, 6);
    }
}
