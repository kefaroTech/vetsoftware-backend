package com.vetsoftware.app.quote.domain;

import java.math.BigDecimal;

/**
 * UN TRAMO del precio vigente del articulo en la tarifa cotizada, leido para
 * congelarlo en la linea.
 *
 * <p>
 * Es <b>un tramo y no "el precio"</b>: desde D-66 los tramos son acumulativos,
 * asi que un articulo con tarifa escalonada produce varios de estos y varias
 * lineas. {@link TieredPrice} es quien decide cuales aplican.
 *
 * @param includedQuantity
 *            unidades que ya vienen incluidas sin cobrar. Se transporta para
 *            que quien construye las lineas pueda restarlas antes de fijar la
 *            cantidad (regla R15): sin esa resta se le cobra al cliente una
 *            unidad que venia incluida. Es propiedad del ARTICULO y no del
 *            tramo, asi que la que manda es la del tramo que arranca en uno.
 * @param tierMin
 *            primera unidad que este tramo cubre. Arranca en 1 y nunca vacio.
 * @param tierMax
 *            ultima unidad cubierta, o {@code null} para "de ahi en adelante".
 */
public record CatalogPriceRef(BigDecimal unitAmount, BigDecimal taxRate, TaxTreatment taxTreatment,
        int includedQuantity, int tierMin, Integer tierMax) {

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
        if (tierMin < 1)
            throw new IllegalArgumentException("tierMin must be 1 or greater");
        if (tierMax != null && tierMax < tierMin)
            throw new IllegalArgumentException("tierMax must not be lower than tierMin");
    }

    /**
     * El tramo unico y abierto: {@code [1, infinito)}. Es la forma que tiene un
     * articulo sin tarifa escalonada, y la que hace que casi todo el catalogo se
     * escriba sin pensar en tramos.
     */
    public CatalogPriceRef(BigDecimal unitAmount, BigDecimal taxRate, TaxTreatment taxTreatment,
            int includedQuantity) {
        this(unitAmount, taxRate, taxTreatment, includedQuantity, 1, null);
    }

    /** Unidades de este tramo que caen dentro de una cantidad facturable dada. */
    public int unitsWithin(int billableQuantity) {
        if (billableQuantity < tierMin)
            return 0;
        int upper = tierMax == null ? billableQuantity : Math.min(billableQuantity, tierMax);
        return upper - tierMin + 1;
    }
}
