package com.vetsoftware.app.subscriptionpayment.domain;

import java.util.Set;

/**
 * Estado de un pago. Espejo de {@code chk_subscription_payments_status}.
 *
 * <p>
 * <strong>Solo {@link #CONFIRMED} cuenta como cobro.</strong> Un pago
 * {@code PENDING} aplicado a una factura no reduce su saldo: la pasarela avisó
 * pero no confirmó, y tratarlo como cobrado es exactamente lo que hace que una
 * clínica que "ya pagó" siga apareciendo en mora — o al revés, que se dé por
 * saldada una factura con dinero que nunca llegó.
 *
 * <p>
 * Las transiciones viven aquí y no en el servicio porque son una invariante del
 * dominio: un pago fallido no se puede confirmar después, y uno devuelto no
 * vuelve a estar vivo. La tabla se lee entera de un vistazo, que es justo lo
 * que un {@code if} encadenado en un service no permite.
 */
public enum SubscriptionPaymentStatus {
    PENDING, CONFIRMED, FAILED, REFUNDED;

    /**
     * Estados a los que este puede pasar. {@code FAILED} y {@code REFUNDED} son
     * terminales a propósito: corregir un pago mal registrado no es reabrirlo, es
     * registrar otro.
     */
    public Set<SubscriptionPaymentStatus> allowedTransitions() {
        return switch (this) {
            case PENDING -> Set.of(CONFIRMED, FAILED);
            case CONFIRMED -> Set.of(REFUNDED);
            case FAILED -> Set.of();
            case REFUNDED -> Set.of();
        };
    }

    public boolean canTransitionTo(SubscriptionPaymentStatus target) {
        return target != null && allowedTransitions().contains(target);
    }
}
