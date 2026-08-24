package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * El cargo ya salio en un documento de cobro. HTTP 409.
 *
 * <p>
 * Un cargo no se re-factura ni se despega de su documento: eso reescribiria el
 * subtotal de un papel ya emitido. Lo que se hace es un cargo negativo que lo
 * compensa, y los dos quedan.
 */
public class SubscriptionChargeAlreadyInvoicedException extends RuntimeException {
    public SubscriptionChargeAlreadyInvoicedException(Long id) {
        super("SubscriptionCharge " + id + " is already invoiced");
    }
}
