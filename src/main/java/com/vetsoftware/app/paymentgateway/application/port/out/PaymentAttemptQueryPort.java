package com.vetsoftware.app.paymentgateway.application.port.out;

import com.vetsoftware.app.paymentgateway.domain.LastPaymentAttempt;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * El historial de intentos de un documento, que es de otra feature
 * ({@code paymentattempt}).
 */
public interface PaymentAttemptQueryPort {

    Optional<LastPaymentAttempt> findLast(Long companyId, Long billingDocumentId);

    /** Intentos imputables desde {@code since}. Ver {@code RetrySchedule}. */
    int countRetryableSince(Long companyId, Long billingDocumentId, LocalDateTime since);
}
