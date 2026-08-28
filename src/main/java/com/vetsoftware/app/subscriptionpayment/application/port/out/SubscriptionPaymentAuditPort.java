package com.vetsoftware.app.subscriptionpayment.application.port.out;

import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import java.math.BigDecimal;

/**
 * Rastro de auditoría del dinero que entra y de cómo se imputa.
 *
 * <p>
 * Los dos hechos que más caro salen sin rastro son {@code CONFIRMED → REFUNDED}
 * —plata que sale— y el reverso de una imputación, que devuelve saldo a una
 * cuenta de cobro que el cliente creía saldada. Con solo {@code http_mutation}
 * ninguno de los dos decía de cuánto.
 *
 * <p>
 * Actor, empresa y origen viajan por el MDC. Ver {@code SubscriptionAuditPort}.
 */
public interface SubscriptionPaymentAuditPort {

    void paymentRegistered(Long paymentId, PaymentMethod method, BigDecimal amount,
            SubscriptionPaymentStatus status);

    void paymentStatusChanged(Long paymentId, SubscriptionPaymentStatus fromStatus,
            SubscriptionPaymentStatus toStatus);

    void documentApplied(Long applicationId, Long documentId, ApplicationSourceKind sourceKind,
            BigDecimal amount);

    void applicationReversed(Long applicationId, Long documentId, BigDecimal amount);
}
