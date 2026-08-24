package com.vetsoftware.app.subscriptionpayment.domain;

import java.math.BigDecimal;

/**
 * Un pago aplicado no se puede marcar como devuelto hasta que sus aplicaciones
 * hayan sido contra-aplicadas. El expediente se corrige con filas de reversa,
 * nunca borrando aplicaciones existentes.
 */
public class SubscriptionPaymentHasActiveApplicationsException extends RuntimeException {

    public SubscriptionPaymentHasActiveApplicationsException(Long paymentId,
            BigDecimal netAppliedAmount) {
        super("SubscriptionPayment " + paymentId + " has active applications for "
                + netAppliedAmount + "; reverse them before refunding the payment");
    }
}
