package com.vetsoftware.app.billingdocumentstatushistory.domain;

public class BillingDocumentStatusHistoryNotFoundException extends RuntimeException {

    public BillingDocumentStatusHistoryNotFoundException(Long id) {
        super("Billing document status history entry not found: " + id);
    }
}
