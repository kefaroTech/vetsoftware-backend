package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * El documento de cobro no existe, o no existe en la empresa del caller. HTTP
 * 404.
 *
 * <p>
 * Las dos cosas dan el mismo error a propósito: distinguirlas le confirmaría a
 * un atacante que el id existe en otra clinica.
 */
public class SubscriptionBillingDocumentNotFoundException extends RuntimeException {
    public SubscriptionBillingDocumentNotFoundException(Long id) {
        super("SubscriptionBillingDocument not found: " + id);
    }
}
