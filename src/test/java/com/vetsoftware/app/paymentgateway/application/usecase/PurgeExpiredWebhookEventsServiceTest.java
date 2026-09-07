package com.vetsoftware.app.paymentgateway.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentgateway.application.port.out.GatewayWebhookEventRecorderPort;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurgeExpiredWebhookEventsService")
class PurgeExpiredWebhookEventsServiceTest {

    private static final LocalDateTime CORTE = LocalDateTime.of(2026, 6, 1, 4, 5);

    @Mock
    private GatewayWebhookEventRecorderPort webhookEventRecorderPort;
    @InjectMocks
    private PurgeExpiredWebhookEventsService service;

    @Test
    @DisplayName("delega la purga en el puerto y devuelve cuantas filas toco")
    void delega_la_purga_en_el_puerto() {
        when(webhookEventRecorderPort.purgeRawBodyOlderThan(CORTE)).thenReturn(7);

        int purgadas = service.purgeRawBodyOlderThan(CORTE);

        assertThat(purgadas).isEqualTo(7);
        verify(webhookEventRecorderPort).purgeRawBodyOlderThan(CORTE);
    }
}
