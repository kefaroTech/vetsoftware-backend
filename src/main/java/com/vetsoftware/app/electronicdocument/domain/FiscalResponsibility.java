package com.vetsoftware.app.electronicdocument.domain;

/**
 * Responsabilidad fiscal del adquiriente congelada en el documento. Companion
 * propio de la feature (vertical slicing): no acopla al enum de {@code owner}.
 * El adaptador del proveedor (MATIAS) lo mapea al {@code tax_level_id}
 * (TaxLevelCode) de la DIAN.
 */
public enum FiscalResponsibility {
    NO_APLICA, // R-99-PN
    GRAN_CONTRIBUYENTE, // O-13
    AUTORRETENEDOR, // O-15
    AGENTE_RETENCION_IVA, // O-23
    REGIMEN_SIMPLE; // O-47

    /**
     * Parsea el nombre del enum (se persiste como String en
     * {@code customer_fiscal_responsibility}) de forma tolerante:
     * {@code null}/blank/desconocido → {@code null} (documentos antiguos sin el
     * dato).
     */
    public static FiscalResponsibility fromName(String name) {
        if (name == null || name.isBlank())
            return null;
        try {
            return valueOf(name.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
