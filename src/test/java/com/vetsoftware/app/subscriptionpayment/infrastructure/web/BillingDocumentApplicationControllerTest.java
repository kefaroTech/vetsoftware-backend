package com.vetsoftware.app.subscriptionpayment.infrastructure.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ApplyBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ListBillingDocumentApplicationsUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ReverseBillingDocumentApplicationUseCase;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BillingDocumentApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("BillingDocumentApplicationController — contrato HTTP")
class BillingDocumentApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ApplyBillingDocumentUseCase applyUseCase;
    @MockitoBean
    private ReverseBillingDocumentApplicationUseCase reverseUseCase;
    @MockitoBean
    private ListBillingDocumentApplicationsUseCase listUseCase;
    @MockitoBean
    private Authz authz;

    @Test
    @DisplayName("lista únicamente las aplicaciones del documento y empresa autenticados")
    void lista_unicamente_las_aplicaciones_del_documento_y_empresa_autenticados() throws Exception {
        when(authz.currentCompanyId()).thenReturn(77L);
        when(listUseCase.listByTargetDocument(9L, 77L, 2, 5)).thenReturn(PageResult.empty(2, 5));

        mockMvc.perform(get("/billing-document-applications").param("targetDocumentId", "9")
                .param("page", "2").param("pageSize", "5")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty()).andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.pageSize").value(5));

        verify(listUseCase).listByTargetDocument(9L, 77L, 2, 5);
    }
}
