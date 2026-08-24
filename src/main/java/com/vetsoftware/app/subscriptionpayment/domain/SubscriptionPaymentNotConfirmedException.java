package com.vetsoftware.app.subscriptionpayment.domain;

/**
 * Se intentó conciliar contra el extracto un pago que la pasarela nunca
 * confirmó. Es el espejo en código de
 * {@code chk_subscription_payments_reconciled}, y existe para que el rechazo
 * llegue como un 409 con mensaje en vez de como una violación de constraint que
 * nadie sabe leer.
 *
 * <p>
 * Lo que evita: plata dada por cuadrada en la cartera sin haber entrado en el
 * banco.
 */
public class SubscriptionPaymentNotConfirmedException extends RuntimeException {
    public SubscriptionPaymentNotConfirmedException(Long id) {
        super("SubscriptionPayment is not CONFIRMED and cannot be reconciled: " + id);
    }
}
