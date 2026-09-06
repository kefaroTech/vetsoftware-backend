package com.vetsoftware.app.paymentgateway.domain;

import java.util.List;

/**
 * Un webhook de Wompi ya interpretado: lo que el caso de uso necesita para
 * decidir, sin que le importe que el transporte fue JSON.
 *
 * @param checksumPropertyValues
 *            los valores —en el mismo orden— de las claves que
 *            {@code signature.properties} declaró, listos para
 *            {@code WompiSignatures.eventChecksum}
 */
public record ParsedWompiEvent(String eventType, String transactionId,
        GatewayTransactionStatus status, String statusMessage, long timestamp,
        List<String> checksumPropertyValues) {

    private static final String TRANSACTION_UPDATED = "transaction.updated";

    public boolean isTransactionUpdated() {
        return TRANSACTION_UPDATED.equals(eventType);
    }
}
