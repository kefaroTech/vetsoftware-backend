package com.vetsoftware.app.paymentgateway.application.usecase;

import com.vetsoftware.app.paymentgateway.application.port.in.PurgeExpiredWebhookEventsUseCase;
import com.vetsoftware.app.paymentgateway.application.port.out.GatewayWebhookEventRecorderPort;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurgeExpiredWebhookEventsService implements PurgeExpiredWebhookEventsUseCase {

    private final GatewayWebhookEventRecorderPort webhookEventRecorderPort;

    public PurgeExpiredWebhookEventsService(
            GatewayWebhookEventRecorderPort webhookEventRecorderPort) {
        this.webhookEventRecorderPort = webhookEventRecorderPort;
    }

    @Override
    @Transactional
    public int purgeRawBodyOlderThan(LocalDateTime cutoff) {
        return webhookEventRecorderPort.purgeRawBodyOlderThan(cutoff);
    }
}
