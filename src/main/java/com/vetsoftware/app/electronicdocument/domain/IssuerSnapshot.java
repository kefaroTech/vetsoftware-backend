package com.vetsoftware.app.electronicdocument.domain;

import java.util.List;

/**
 * Copia congelada de la identidad fiscal del emisor al momento de emitir. No es
 * una referencia viva a CompanyTaxProfile: si el perfil cambia luego, el
 * documento conserva esta copia.
 *
 * <p>
 * <b>La copia no lo cubre todo, y por eso ademas se apunta a la fila.</b> Los
 * seis campos de arriba son los que el documento congela desde los changesets
 * 121/136; el <b>nombre comercial</b>, la <b>actividad economica</b> y el
 * conjunto completo de la ficha vigente no viajaban en ninguna foto y se leian
 * de la fila viva, asi que un cambio de RUT los reescribia hacia atras. Desde
 * el changeset 364 {@code company_tax_profiles} lleva vigencia semiabierta y
 * {@code electronic_documents.company_tax_profile_id} enlaza con la fila exacta
 * con la que se emitio: {@link #companyTaxProfileId} es ese enlace.
 */
public record IssuerSnapshot(String documentType, String documentId, String verificationDigit,
        String legalName, String taxRegime, String email,
        /**
         * Códigos de responsabilidad fiscal del RUT (p. ej. O-13, O-15, R-99-PN),
         * congelados al emitir.
         */
        List<String> responsibilities,
        /**
         * La fila de {@code company_tax_profiles} con la que se emitio. Nulo cuando la
         * empresa emitio sin ficha fiscal completa (dato hoy posible), que es por lo
         * que la columna y su clave foranea son nullables. Una nota hereda el del
         * documento original: se emite bajo la misma identidad que la factura que
         * corrige.
         */
        Long companyTaxProfileId) {
    public IssuerSnapshot {
        if (documentId == null || documentId.isBlank())
            throw new IllegalArgumentException("issuer documentId is required");
        if (legalName == null || legalName.isBlank())
            throw new IllegalArgumentException("issuer legalName is required");
        responsibilities = responsibilities == null ? List.of() : List.copyOf(responsibilities);
    }

    /**
     * Identidad sin la fila de perfil que la origino. Es la forma legitima cuando
     * el emisor no tiene ficha con la que enlazar; para el camino de emision real
     * usa siempre el constructor completo, porque el enlace es la mitad de la
     * congelacion.
     */
    public IssuerSnapshot(String documentType, String documentId, String verificationDigit,
            String legalName, String taxRegime, String email, List<String> responsibilities) {
        this(documentType, documentId, verificationDigit, legalName, taxRegime, email,
                responsibilities, null);
    }
}
