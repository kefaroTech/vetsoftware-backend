package com.vetsoftware.app.paymentgateway.application.port.out;

import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Anota un cobro rechazado, delegando en {@code paymentattempt}. */
public interface PaymentAttemptRecorderPort {
    void record(Long companyId, Long billingDocumentId, Long paymentMethodId, String gateway,
            BigDecimal requestedAmount, String gatewayDeclineCode, GatewayDeclineKind declineKind,
            LocalDateTime attemptedAt, LocalDateTime nextAttemptAt);
}
