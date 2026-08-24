package com.vetsoftware.app.subscriptionbilling.infrastructure.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionbilling.application.port.in.CreateSubscriptionChargeUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.GenerateBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.IssueCreditNoteUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.ListBillingDocumentsAwaitingExternalUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.ListOverdueBillingDocumentsUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.RegisterExternalInvoiceUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.SubmitBillingDocumentForExternalIssueUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.VoidBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.VoidSubscriptionChargeUseCase;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SystemSubscriptionBillingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemSubscriptionBillingController — contrato HTTP")
class SystemSubscriptionBillingControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CreateSubscriptionChargeUseCase createChargeUseCase;
    @MockitoBean
    private VoidSubscriptionChargeUseCase voidChargeUseCase;
    @MockitoBean
    private GenerateBillingDocumentUseCase generateUseCase;
    @MockitoBean
    private SubmitBillingDocumentForExternalIssueUseCase submitUseCase;
    @MockitoBean
    private RegisterExternalInvoiceUseCase registerExternalUseCase;
    @MockitoBean
    private VoidBillingDocumentUseCase voidDocumentUseCase;
    @MockitoBean
    private IssueCreditNoteUseCase creditNoteUseCase;
    @MockitoBean
    private ListBillingDocumentsAwaitingExternalUseCase awaitingUseCase;
    @MockitoBean
    private ListOverdueBillingDocumentsUseCase overdueUseCase;
    @MockitoBean
    private Authz authz;

    @Test
    @DisplayName("expone el barrido global de documentos vencidos")
    void expone_el_barrido_global_de_documentos_vencidos() throws Exception {
        when(overdueUseCase.listOverdue(5, 12)).thenReturn(PageResult.empty(5, 12));

        mockMvc.perform(get("/system/subscription-billing/documents/overdue").param("page", "5")
                .param("pageSize", "12")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty()).andExpect(jsonPath("$.page").value(5))
                .andExpect(jsonPath("$.pageSize").value(12));

        verify(overdueUseCase).listOverdue(5, 12);
    }
}
