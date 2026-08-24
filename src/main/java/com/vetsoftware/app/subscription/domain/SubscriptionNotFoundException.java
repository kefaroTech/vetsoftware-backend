package com.vetsoftware.app.subscription.domain;

/**
 * Subscription inexistente o de otra empresa. GlobalExceptionHandler:
 * <strong>404</strong>.
 */
public class SubscriptionNotFoundException extends RuntimeException {
    public SubscriptionNotFoundException(Long id) {
        super("Subscription not found: " + id);
    }
}
