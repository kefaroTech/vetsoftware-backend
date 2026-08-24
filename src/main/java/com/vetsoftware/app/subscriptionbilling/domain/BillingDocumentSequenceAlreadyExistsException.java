package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * Ya hay una serie con ese prefijo (uq_billing_document_sequences_prefix). HTTP
 * 409.
 */
public class BillingDocumentSequenceAlreadyExistsException extends RuntimeException {
    public BillingDocumentSequenceAlreadyExistsException(String prefix) {
        super("BillingDocumentSequence already exists for prefix: " + prefix);
    }
}
