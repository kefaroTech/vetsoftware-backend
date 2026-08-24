package com.vetsoftware.app.subscriptionpayment.domain;

public class BillingDocumentApplicationNotFoundException extends RuntimeException {
    public BillingDocumentApplicationNotFoundException(Long id) {
        super("BillingDocumentApplication not found: " + id);
    }
}
