package com.vetsoftware.app.paymentgateway.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentgateway.application.dto.WompiWebhookEventDto;
import com.vetsoftware.app.paymentgateway.application.port.out.WompiWebhookEventQueryPort;
import com.vetsoftware.app.paymentgateway.application.query.ListWompiWebhookEventsQuery;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListWompiWebhookEventsService - rastro de webhooks")
class ListWompiWebhookEventsServiceTest {

    @Mock
    private WompiWebhookEventQueryPort queryPort;
    @InjectMocks
    private ListWompiWebhookEventsService service;

    @Test
    @DisplayName("delega el filtro entero en el puerto de consulta")
    void delega_en_el_puerto() {
        ListWompiWebhookEventsQuery query = new ListWompiWebhookEventsQuery("TX-2026-0001", 42L,
                LocalDateTime.of(2026, 8, 1, 0, 0), LocalDateTime.of(2026, 8, 31, 23, 59), 0, 20);
        WompiWebhookEventDto event = new WompiWebhookEventDto(1L, "WOMPI", "transaction.updated",
                "TX-2026-0001", "checksum-1", "APPLIED", LocalDateTime.of(2026, 8, 23, 10, 0),
                LocalDateTime.of(2026, 8, 23, 10, 1), LocalDateTime.of(2026, 8, 23, 10, 0), "{}");
        when(queryPort.search(query)).thenReturn(PageResult.of(List.of(event), 0, 20, 1));

        PageResult<WompiWebhookEventDto> result = service.search(query);

        assertThat(result.content()).containsExactly(event);
        verify(queryPort).search(query);
        verifyNoMoreInteractions(queryPort);
    }
}
