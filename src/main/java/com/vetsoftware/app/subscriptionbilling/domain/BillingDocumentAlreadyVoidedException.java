package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * El documento ya estaba anulado; anularlo otra vez no significa nada. HTTP
 * 409.
 */
public class BillingDocumentAlreadyVoidedException extends RuntimeException {
    public BillingDocumentAlreadyVoidedException(Long id) {
        super("Billing document " + id + " is already voided");
    }
}
