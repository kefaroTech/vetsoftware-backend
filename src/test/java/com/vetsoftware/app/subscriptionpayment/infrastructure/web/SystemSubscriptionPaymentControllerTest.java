package com.vetsoftware.app.subscriptionpayment.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ExportSubscriptionPaymentsUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ListAllSubscriptionPaymentsUseCase;
import com.vetsoftware.app.subscriptionpayment.application.query.ListAllSubscriptionPaymentsQuery;
import com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SystemSubscriptionPaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemSubscriptionPaymentController - contrato HTTP cross-tenant")
class SystemSubscriptionPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ListAllSubscriptionPaymentsUseCase listUseCase;
    @MockitoBean
    private ExportSubscriptionPaymentsUseCase exportUseCase;

    @Test
    @DisplayName("expone companyId y propaga filtro y pagina")
    void expone_company_id_y_paginacion() throws Exception {
        SubscriptionPaymentDto payment = SubscriptionPaymentDto
                .from(SubscriptionPaymentMother.pagoPendiente());
        when(listUseCase.listAll(new ListAllSubscriptionPaymentsQuery(
                SubscriptionPaymentMother.EMPRESA, null, null, null, null, 2, 5)))
                .thenReturn(PageResult.of(List.of(payment), 2, 5, 11));

        mockMvc.perform(get("/system/subscription-payments")
                .param("companyId", SubscriptionPaymentMother.EMPRESA.toString()).param("page", "2")
                .param("pageSize", "5")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(
                        jsonPath("$.content[0].companyId").value(SubscriptionPaymentMother.EMPRESA))
                .andExpect(jsonPath("$.page").value(2)).andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.totalElements").value(11));

        verify(listUseCase).listAll(new ListAllSubscriptionPaymentsQuery(
                SubscriptionPaymentMother.EMPRESA, null, null, null, null, 2, 5));
    }

    @Test
    @DisplayName("propaga pendingOlderThanMinutes")
    void propaga_pending_older_than_minutes() throws Exception {
        when(listUseCase.listAll(any())).thenReturn(PageResult.empty(0, 20));

        mockMvc.perform(get("/system/subscription-payments").param("pendingOlderThanMinutes", "30"))
                .andExpect(status().isOk());

        verify(listUseCase)
                .listAll(new ListAllSubscriptionPaymentsQuery(null, null, null, null, 30, 0, 20));
    }

    @Test
    @DisplayName("export devuelve un CSV descargable")
    void export_devuelve_csv() throws Exception {
        SubscriptionPaymentDto payment = SubscriptionPaymentDto
                .from(SubscriptionPaymentMother.pagoConfirmado("500000.00"));
        when(exportUseCase.export(any())).thenReturn(List.of(payment));

        mockMvc.perform(get("/system/subscription-payments/export").param("companyId",
                SubscriptionPaymentMother.EMPRESA.toString())).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"));

        verify(exportUseCase).export(new ListAllSubscriptionPaymentsQuery(
                SubscriptionPaymentMother.EMPRESA, null, null, null, null, 0, 0));
    }
}
