package com.vetsoftware.app.paymentgateway.application.port.out;

import com.vetsoftware.app.paymentgateway.domain.GatewayWebhookOutcome;
import java.time.LocalDateTime;

/**
 * Rastro auditable de todo webhook de Wompi <strong>autenticado</strong>, se
 * procese o no: un checksum inválido no llega a persistirse. La idempotencia
 * por {@code (gateway, event_checksum)} vive en la unicidad de esa tabla:
 * {@link #existsByChecksum} se consulta antes de insertar para no reprocesar un
 * evento ya recibido.
 */
public interface GatewayWebhookEventRecorderPort {

    boolean existsByChecksum(String gateway, String eventChecksum);

    Long recordReceived(String gateway, String eventType, String eventChecksum,
            String gatewayReference, LocalDateTime receivedAt, String rawBody);

    void recordOutcome(Long eventId, LocalDateTime processedAt, GatewayWebhookOutcome outcome);

    /**
     * Vacía {@code raw_body} de los eventos anteriores a {@code cutoff}; devuelve
     * cuántas filas tocó.
     */
    int purgeRawBodyOlderThan(LocalDateTime cutoff);
}
