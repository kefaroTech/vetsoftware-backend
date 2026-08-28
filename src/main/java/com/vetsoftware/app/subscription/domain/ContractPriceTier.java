package com.vetsoftware.app.subscription.domain;

import java.math.BigDecimal;

/**
 * UN TRAMO de la tarifa publicada, tal y como se lee para firmar una linea.
 *
 * <p>
 * Desde D-66 los tramos son <b>acumulativos</b>: cada uno se cobra a su propio
 * precio y una cantidad escalonada se firma como varias lineas del mismo
 * articulo. Este tipo es el insumo de {@link ContractPriceTiers}, que es quien
 * decide cuales aplican y con cuantas unidades cada uno.
 *
 * @param includedQuantity
 *            lo que el ARTICULO incluye sin cobrar. Es propiedad del articulo y
 *            no del tramo, asi que la que manda es la del tramo que arranca en
 *            uno.
 * @param tierMax
 *            {@code null} es "de ahi en adelante".
 */
public record ContractPriceTier(int tierMin, Integer tierMax, int includedQuantity,
        TaxTreatment taxTreatment, BigDecimal unitAmount, BigDecimal taxRate) {

    public ContractPriceTier {
        if (tierMin < 1)
            throw new IllegalArgumentException("tierMin must be 1 or greater");
        if (tierMax != null && tierMax < tierMin)
            throw new IllegalArgumentException("tierMax must not be lower than tierMin");
        if (includedQuantity < 0)
            throw new IllegalArgumentException("includedQuantity cannot be negative");
        if (taxTreatment == null)
            throw new IllegalArgumentException("taxTreatment is required");
        if (unitAmount == null || unitAmount.signum() < 0)
            throw new IllegalArgumentException("unitAmount must be zero or positive");
        if (taxRate == null || taxRate.signum() < 0)
            throw new IllegalArgumentException("taxRate must be zero or positive");
    }

    /** Unidades de este tramo que caen dentro de una cantidad facturable dada. */
    public int unitsWithin(int billableQuantity) {
        if (billableQuantity < tierMin)
            return 0;
        int upper = tierMax == null ? billableQuantity : Math.min(billableQuantity, tierMax);
        return upper - tierMin + 1;
    }
}
