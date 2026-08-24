package com.vetsoftware.app.subscriptionbilling.infrastructure.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionbilling.application.port.in.CreateBillingDocumentSequenceUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.ListBillingDocumentSequencesUseCase;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BillingDocumentSequenceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("BillingDocumentSequenceController — contrato HTTP")
class BillingDocumentSequenceControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CreateBillingDocumentSequenceUseCase createUseCase;
    @MockitoBean
    private ListBillingDocumentSequencesUseCase listUseCase;

    @Test
    @DisplayName("expone el consecutivo global sin filtro de empresa")
    void expone_el_consecutivo_global_sin_filtro_de_empresa() throws Exception {
        when(listUseCase.listAll(1, 10)).thenReturn(PageResult.empty(1, 10));

        mockMvc.perform(get("/system/billing-document-sequences").param("page", "1")
                .param("pageSize", "10")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty()).andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(10));

        verify(listUseCase).listAll(1, 10);
    }
}
