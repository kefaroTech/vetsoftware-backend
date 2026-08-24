package com.vetsoftware.app.subscription.domain;

/**
 * SubscriptionItem inexistente o de otra empresa. GlobalExceptionHandler:
 * <strong>404</strong>.
 */
public class SubscriptionItemNotFoundException extends RuntimeException {
    public SubscriptionItemNotFoundException(Long id) {
        super("SubscriptionItem not found: " + id);
    }
}
