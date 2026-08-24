package com.vetsoftware.app.quote.domain;

import java.math.BigDecimal;

/**
 * Precio vigente del articulo en la tarifa cotizada, leido UNA SOLA VEZ para
 * congelarlo en la linea.
 *
 * @param includedQuantity
 *            unidades que ya vienen incluidas sin cobrar. Se transporta para
 *            que quien construye las lineas pueda restarlas antes de fijar la
 *            cantidad (regla R15): sin esa resta se le cobra al cliente una
 *            unidad que venia incluida.
 */
public record CatalogPriceRef(BigDecimal unitAmount, BigDecimal taxRate, TaxTreatment taxTreatment,
        int includedQuantity) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public CatalogPriceRef {
        if (unitAmount == null || unitAmount.signum() < 0)
            throw new IllegalArgumentException("unitAmount must be zero or positive");
        if (taxRate == null || taxRate.signum() < 0 || taxRate.compareTo(HUNDRED) > 0)
            throw new IllegalArgumentException("taxRate must be between 0 and 100");
        if (taxTreatment == null)
            throw new IllegalArgumentException("taxTreatment is required");
        if (includedQuantity < 0)
            throw new IllegalArgumentException("includedQuantity cannot be negative");
    }
}
