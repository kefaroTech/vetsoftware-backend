package com.vetsoftware.app.subscriptionpayment.infrastructure.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ListAllSubscriptionPaymentsUseCase;
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

    @Test
    @DisplayName("expone companyId y propaga filtro y pagina")
    void expone_company_id_y_paginacion() throws Exception {
        SubscriptionPaymentDto payment = SubscriptionPaymentDto
                .from(SubscriptionPaymentMother.pagoPendiente());
        when(listUseCase.listAll(SubscriptionPaymentMother.EMPRESA, 2, 5))
                .thenReturn(PageResult.of(List.of(payment), 2, 5, 11));

        mockMvc.perform(get("/system/subscription-payments")
                .param("companyId", SubscriptionPaymentMother.EMPRESA.toString()).param("page", "2")
                .param("pageSize", "5")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(
                        jsonPath("$.content[0].companyId").value(SubscriptionPaymentMother.EMPRESA))
                .andExpect(jsonPath("$.page").value(2)).andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.totalElements").value(11));

        verify(listUseCase).listAll(SubscriptionPaymentMother.EMPRESA, 2, 5);
    }
}
