package com.vetsoftware.app.externalinvoicingoutage.domain;

/** No existe esa caida. 404. */
public class ExternalInvoicingOutageNotFoundException extends RuntimeException {

    public ExternalInvoicingOutageNotFoundException(Long id) {
        super("External invoicing outage not found: " + id);
    }
}
