package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * No hay serie declarada para ese prefijo. HTTP 404.
 *
 * <p>
 * No se crea sola. Un consecutivo que se autocrea al primer uso arranca en 1
 * sin que nadie lo haya decidido, y si la serie ya existia con otro nombre
 * acaban conviviendo dos numeraciones para el mismo tipo de documento.
 */
public class BillingDocumentSequenceNotFoundException extends RuntimeException {
    public BillingDocumentSequenceNotFoundException(String prefix) {
        super("BillingDocumentSequence not found for prefix: " + prefix);
    }
}
