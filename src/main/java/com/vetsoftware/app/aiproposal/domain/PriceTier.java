package com.vetsoftware.app.aiproposal.domain;

import java.math.BigDecimal;

/**
 * Un escalon de {@code catalog_prices}: el precio unitario que se cobra a las
 * unidades comprendidas entre {@code tierMin} y {@code tierMax}.
 *
 * <p>
 * <strong>{@code tierMax} nulo es "hasta el infinito"</strong>, y es como
 * termina toda escalera sana: la semilla {@code 310_seed_price_list_2026}
 * declara por escrito que los tramos "encadenan SIN SALTO desde 1 hasta un
 * ultimo tramo abierto". {@link PriceLadder} lo comprueba en vez de confiar.
 *
 * <p>
 * <strong>El impuesto viaja en el tramo, no en el articulo.</strong> Es
 * {@code catalog_prices.tax_rate}, y por eso el 19 % no esta cableado en ningun
 * sitio de esta rodaja: hay articulos exentos y el {@code CHECK}
 * {@code chk_catalog_prices_tax_coherence} permite {@code EXEMPT} con tasa
 * cero.
 */
public record PriceTier(int tierMin, Integer tierMax, int includedQuantity, BigDecimal unitAmount,
        BigDecimal taxRate) {

    public PriceTier {
        if (tierMin < 1)
            throw new IllegalArgumentException("tierMin must be at least 1: " + tierMin);
        if (tierMax != null && tierMax < tierMin)
            throw new IllegalArgumentException(
                    "tierMax cannot precede tierMin: " + tierMin + ".." + tierMax);
        if (includedQuantity < 0)
            throw new IllegalArgumentException(
                    "includedQuantity cannot be negative: " + includedQuantity);
        if (unitAmount == null || unitAmount.signum() < 0)
            throw new IllegalArgumentException("tier unitAmount must be zero or positive");
        if (taxRate == null || taxRate.signum() < 0)
            throw new IllegalArgumentException("tier taxRate must be zero or positive");
    }

    /** El tramo abierto es el ultimo y no tiene techo. */
    public boolean esAbierto() {
        return tierMax == null;
    }
}
