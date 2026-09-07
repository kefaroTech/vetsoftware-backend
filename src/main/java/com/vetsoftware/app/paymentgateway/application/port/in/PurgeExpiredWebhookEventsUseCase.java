package com.vetsoftware.app.paymentgateway.application.port.in;

import java.time.LocalDateTime;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El barrido de retención que vacía el cuerpo crudo de los webhooks de Wompi
 * más allá del plazo configurado. Cross-tenant a propósito, mismo criterio que
 * {@code ReconcilePendingPaymentsUseCase}
 * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}).
 */
public interface PurgeExpiredWebhookEventsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    int purgeRawBodyOlderThan(LocalDateTime cutoff);
}
