package com.vetsoftware.app.externalinvoicereconciliation.domain;

public class ExternalInvoiceReconciliationNotFoundException extends RuntimeException {

    public ExternalInvoiceReconciliationNotFoundException(Long id) {
        super("External invoice reconciliation not found: " + id);
    }
}
