package com.vetsoftware.app.owner.domain;

/**
 * Responsabilidad fiscal del adquiriente (RUT) → {@code tax_level_id} de la DIAN/MATIAS ({@code TaxLevelCode}
 * del XML). NO_APLICA (R-99-PN) es el caso por defecto (personas naturales / consumidor); el resto solo aplica
 * a empresas con responsabilidades especiales. El mapeo al id de MATIAS lo hace el adaptador del proveedor.
 */
public enum FiscalResponsibility {
    NO_APLICA,             // R-99-PN
    GRAN_CONTRIBUYENTE,    // O-13
    AUTORRETENEDOR,        // O-15
    AGENTE_RETENCION_IVA,  // O-23
    REGIMEN_SIMPLE;        // O-47

    /**
     * Valor por defecto cuando no se captura explícitamente: la responsabilidad fiscal es declarativa (no se
     * infiere del tipo de persona/documento), y la inmensa mayoría de adquirientes no tiene una especial.
     */
    public static FiscalResponsibility defaultValue() {
        return NO_APLICA;
    }
}
