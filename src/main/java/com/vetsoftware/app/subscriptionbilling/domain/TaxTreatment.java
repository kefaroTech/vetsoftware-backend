package com.vetsoftware.app.subscriptionbilling.domain;

import java.math.BigDecimal;

/**
 * Cómo trata el impuesto una línea.
 *
 * <p>
 * <b>Excluido y exento no son lo mismo y no se pueden colapsar en «tarifa
 * cero».</b> Se declaran distinto y dan derechos distintos: el exento está
 * gravado a tarifa cero y conserva el derecho a descontar el IVA pagado; el
 * excluido está fuera del impuesto y no lo conserva. Un modelo que los mezcle
 * en {@code tax_rate = 0} pierde información que después no se puede
 * reconstruir, y por eso {@code subscription_charges.tax_treatment} existe como
 * columna aparte de la tarifa.
 *
 * <p>
 * Espejo de {@code chk_subscription_charges_tax_treatment} y de
 * {@code chk_sbdt_tax_treatment}.
 */
public enum TaxTreatment {
    /** Gravado. Exige {@code tax_rate > 0}. */
    TAXED,
    /** Exento: gravado a tarifa cero, con derecho a descuento. */
    EXEMPT,
    /** Excluido: fuera del impuesto, sin derecho a descuento. */
    EXCLUDED;

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    /**
     * Espejo de {@code chk_sbdt_coherence}, aplicado también al cargo aunque su
     * propio {@code CHECK} no llegue a tanto.
     *
     * <p>
     * Es deliberadamente más estricto que
     * {@code chk_subscription_charges_tax_rate}, y el motivo es concreto: un cargo
     * {@code TAXED} con tarifa 0 es <b>inconstruible aguas abajo</b> — el desglose
     * que agrupa por {@code (tratamiento, tarifa)} produciría una fila
     * {@code (TAXED, 0.00)} que {@code chk_sbdt_coherence} rechaza, y el fallo
     * aparecería al cerrar el documento, no al devengar. Fallar aquí señala el
     * campo; fallar allí es una violación de constraint convertida en un 500 a
     * mitad del cierre mensual.
     */
    public void validarTarifa(BigDecimal taxRate) {
        if (taxRate == null)
            throw new IllegalArgumentException("taxRate is required");
        if (taxRate.signum() < 0 || taxRate.compareTo(CIEN) > 0)
            throw new IllegalArgumentException("taxRate must be between 0 and 100");
        if (this == TAXED && taxRate.signum() <= 0)
            throw new IllegalArgumentException("TAXED requires a tax rate greater than zero");
        if (this != TAXED && taxRate.signum() != 0)
            throw new IllegalArgumentException(this + " requires a tax rate of zero");
    }
}
