package com.vetsoftware.app.electronicdocument.domain;

import java.util.List;

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
        String email,
        /** Códigos de responsabilidad fiscal del RUT (p. ej. O-13, O-15, R-99-PN), congelados al emitir. */
        List<String> responsibilities
) {
    public IssuerSnapshot {
        if (documentId == null || documentId.isBlank())
            throw new IllegalArgumentException("issuer documentId is required");
        if (legalName == null || legalName.isBlank())
            throw new IllegalArgumentException("issuer legalName is required");
        responsibilities = responsibilities == null ? List.of() : List.copyOf(responsibilities);
    }
}
