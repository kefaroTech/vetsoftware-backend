package com.vetsoftware.app.paymentgateway.infrastructure.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.paymentgateway.application.dto.WompiWebhookEventDto;
import com.vetsoftware.app.paymentgateway.application.port.in.ListWompiWebhookEventsUseCase;
import com.vetsoftware.app.paymentgateway.application.query.ListWompiWebhookEventsQuery;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WompiWebhookEventController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("WompiWebhookEventController - rastro de webhooks para disputas")
class WompiWebhookEventControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ListWompiWebhookEventsUseCase listUseCase;

    @Test
    @DisplayName("propaga referencia, companyId y rango, y expone rawBody")
    void propaga_filtros_y_expone_raw_body() throws Exception {
        WompiWebhookEventDto event = new WompiWebhookEventDto(1L, "WOMPI", "transaction.updated",
                "TX-2026-0001", "checksum-1", "APPLIED", LocalDateTime.of(2026, 8, 23, 10, 0),
                LocalDateTime.of(2026, 8, 23, 10, 1), LocalDateTime.of(2026, 8, 23, 10, 0),
                "{\"event\":\"transaction.updated\"}");
        when(listUseCase
                .search(new ListWompiWebhookEventsQuery("TX-2026-0001", 42L, null, null, 0, 20)))
                .thenReturn(PageResult.of(List.of(event), 0, 20, 1));

        mockMvc.perform(get("/system/payment-gateway/wompi/events")
                .param("reference", "TX-2026-0001").param("companyId", "42"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].gatewayReference").value("TX-2026-0001"))
                .andExpect(jsonPath("$.content[0].processingOutcome").value("APPLIED"))
                .andExpect(jsonPath("$.content[0].rawBody")
                        .value("{\"event\":\"transaction.updated\"}"));

        verify(listUseCase)
                .search(new ListWompiWebhookEventsQuery("TX-2026-0001", 42L, null, null, 0, 20));
    }

    @Test
    @DisplayName("rawBody nulo cuando el evento ya se purgo")
    void raw_body_nulo_cuando_se_purgo() throws Exception {
        WompiWebhookEventDto event = new WompiWebhookEventDto(2L, "WOMPI", "transaction.updated",
                "TX-2026-0002", "checksum-2", "APPLIED", LocalDateTime.of(2026, 8, 23, 10, 0),
                LocalDateTime.of(2026, 8, 23, 10, 1), LocalDateTime.of(2026, 8, 23, 10, 0), null);
        when(listUseCase.search(new ListWompiWebhookEventsQuery(null, null, null, null, 0, 20)))
                .thenReturn(PageResult.of(List.of(event), 0, 20, 1));

        mockMvc.perform(get("/system/payment-gateway/wompi/events")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rawBody").doesNotExist());
    }
}
