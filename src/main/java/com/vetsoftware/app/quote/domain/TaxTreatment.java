package com.vetsoftware.app.quote.domain;

/**
 * Tratamiento fiscal congelado en la linea (chk_quote_lines_tax_treatment).
 *
 * <p>
 * EXEMPT y EXCLUDED NO se pueden colapsar en "tarifa cero": excluido y exento
 * se declaran distinto y dan derechos distintos ante la DIAN. Sin esta columna,
 * tax_rate = 0 es ambiguo entre los tres.
 */
public enum TaxTreatment {
    TAXED, EXEMPT, EXCLUDED
}
