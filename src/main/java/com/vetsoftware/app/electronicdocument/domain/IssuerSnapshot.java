package com.vetsoftware.app.electronicdocument.domain;

/**
 * Copia congelada de la identidad fiscal del emisor al momento de emitir.
 * No es una referencia viva a CompanyTaxProfile: si el perfil cambia luego, el documento conserva esta copia.
 */
public record IssuerSnapshot(
        String documentType,
        String documentId,
        String verificationDigit,
        String legalName,
        String taxRegime,
        String email
) {
    public IssuerSnapshot {
        if (documentId == null || documentId.isBlank())
            throw new IllegalArgumentException("issuer documentId is required");
        if (legalName == null || legalName.isBlank())
            throw new IllegalArgumentException("issuer legalName is required");
    }
}
