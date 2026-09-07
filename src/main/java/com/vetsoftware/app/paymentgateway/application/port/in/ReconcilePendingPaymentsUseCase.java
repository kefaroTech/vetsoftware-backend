package com.vetsoftware.app.paymentgateway.application.port.in;

import com.vetsoftware.app.paymentgateway.application.dto.ReconciliationBatchResult;
import java.time.LocalDateTime;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El barrido de conciliación de pagos {@code PENDING} envejecidos: uno de los
 * barridos de plataforma. Cross-tenant a propósito, mismo criterio que
 * {@link RunPaymentCollectionUseCase}
 * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}).
 */
public interface ReconcilePendingPaymentsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    ReconciliationBatchResult reconcileOlderThan(LocalDateTime cutoff, int limit);
}
