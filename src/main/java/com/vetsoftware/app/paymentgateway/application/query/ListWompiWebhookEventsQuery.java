package com.vetsoftware.app.paymentgateway.application.query;

import java.time.LocalDateTime;

/**
 * {@code companyId} no es una columna propia de {@code gateway_webhook_events}
 * —el evento llega antes de saber a que empresa pertenece (ver
 * {@code WompiWebhookEventJpaEntity})—: se resuelve por correlación con
 * {@code subscription_payments} a través del par {@code (gateway,
 * gatewayReference)}, que es global y único.
 */
public record ListWompiWebhookEventsQuery(String reference, Long companyId, LocalDateTime from,
        LocalDateTime to, int page, int pageSize) {
}
