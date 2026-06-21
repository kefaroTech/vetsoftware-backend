package com.vetsoftware.app.owner.domain;

/**
 * Régimen de IVA del adquiriente (responsabilidad frente al IVA). Mapea al {@code tax_regime_id} de la
 * DIAN/MATIAS: RESPONSABLE_IVA = 1, NO_RESPONSABLE_IVA = 2.
 */
public enum TaxRegime {
    RESPONSABLE_IVA,
    NO_RESPONSABLE_IVA;

    /**
     * Valor por defecto cuando no se captura explícitamente: se considera Responsable de IVA si es persona
     * jurídica o se identifica con NIT (negocios registrados); el resto (CC, CE, pasaporte…) No responsable.
     */
    public static TaxRegime defaultFor(PersonType personType, OwnerDocumentType documentType) {
        boolean responsable = personType == PersonType.JURIDICA || documentType == OwnerDocumentType.NIT;
        return responsable ? RESPONSABLE_IVA : NO_RESPONSABLE_IVA;
    }
}
