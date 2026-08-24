package com.vetsoftware.app.subscriptionbilling.domain;

/** El cargo no existe, o no existe en la empresa del caller. HTTP 404. */
public class SubscriptionChargeNotFoundException extends RuntimeException {
    public SubscriptionChargeNotFoundException(Long id) {
        super("SubscriptionCharge not found: " + id);
    }
}
