package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import com.vetsoftware.app.paymentgateway.application.port.out.GatewayWebhookEventRecorderPort;
import com.vetsoftware.app.paymentgateway.domain.GatewayWebhookOutcome;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class JpaGatewayWebhookEventRecorderPort implements GatewayWebhookEventRecorderPort {

    private final WompiWebhookEventJpaRepository repository;

    public JpaGatewayWebhookEventRecorderPort(WompiWebhookEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByChecksum(String gateway, String eventChecksum) {
        return repository.existsByGatewayAndEventChecksum(gateway, eventChecksum);
    }

    @Override
    public Long recordReceived(String gateway, String eventType, String eventChecksum,
            String gatewayReference, LocalDateTime receivedAt, String rawBody) {
        WompiWebhookEventJpaEntity entity = new WompiWebhookEventJpaEntity();
        entity.setGateway(gateway);
        entity.setEventType(eventType);
        entity.setEventChecksum(eventChecksum);
        entity.setGatewayReference(gatewayReference);
        entity.setReceivedAt(receivedAt);
        entity.setRawBody(rawBody);
        entity.setCreatedDate(receivedAt);
        return repository.save(entity).getId();
    }

    @Override
    public void recordOutcome(Long eventId, LocalDateTime processedAt,
            GatewayWebhookOutcome outcome) {
        WompiWebhookEventJpaEntity entity = repository.findById(eventId).orElseThrow(
                () -> new IllegalStateException("No se encontró el evento de webhook " + eventId));
        entity.setProcessedAt(processedAt);
        entity.setProcessingOutcome(outcome.name());
        repository.save(entity);
    }

    @Override
    public int purgeRawBodyOlderThan(LocalDateTime cutoff) {
        return repository.purgeRawBodyReceivedBefore(cutoff);
    }
}
