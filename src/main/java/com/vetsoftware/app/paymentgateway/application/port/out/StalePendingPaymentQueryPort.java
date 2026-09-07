package com.vetsoftware.app.paymentgateway.application.port.out;

import com.vetsoftware.app.paymentgateway.domain.StalePendingPayment;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Pagos {@code PENDING} de pasarela envejecidos, para el barrido de
 * conciliación. Cross-tenant a propósito: el barrido recorre todas las
 * clínicas, igual que {@code RunPaymentCollectionUseCase}.
 */
public interface StalePendingPaymentQueryPort {
    List<StalePendingPayment> findOlderThan(LocalDateTime cutoff, int limit);
}
