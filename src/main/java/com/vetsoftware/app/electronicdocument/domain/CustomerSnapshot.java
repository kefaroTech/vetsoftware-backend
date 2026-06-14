package com.vetsoftware.app.electronicdocument.domain;

/**
 * Copia congelada de la identidad fiscal del adquiriente al momento de emitir.
 * No es una referencia viva a Owner: si el dueno cambia luego, el documento conserva esta copia.
 */
public record CustomerSnapshot(
        String documentType,
        String documentId,
        String verificationDigit,
        String personType,
        String legalName,
        String name
) {
    public CustomerSnapshot {
        if (documentId == null || documentId.isBlank())
            throw new IllegalArgumentException("customer documentId is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("customer name is required");
    }
}
