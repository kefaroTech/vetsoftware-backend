package com.vetsoftware.app.electronicdocument.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcula las retenciones que el adquiriente (agente retenedor) practica sobre la venta, usando las tarifas
 * configuradas por el emisor (WithholdingConfig). ReteFuente y ReteICA aplican sobre la base gravable;
 * ReteIVA aplica sobre el IVA generado. Devuelve {@link WithholdingAmounts#NONE} si el adquiriente no es
 * agente retenedor (las tarifas en cero también producen cero).
 */
public final class WithholdingCalculator {
    private static final int MONEY_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private WithholdingCalculator() {}

    public static WithholdingAmounts compute(boolean withholdingAgent, BigDecimal taxableBase,
                                             BigDecimal ivaAmount, BigDecimal reteFuenteRate,
                                             BigDecimal reteIvaRate, BigDecimal reteIcaRate) {
        if (!withholdingAgent) return WithholdingAmounts.NONE;
        return new WithholdingAmounts(
                percentOf(taxableBase, reteFuenteRate),
                percentOf(ivaAmount, reteIvaRate),
                percentOf(taxableBase, reteIcaRate));
    }

    private static BigDecimal percentOf(BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null || rate.signum() <= 0) return BigDecimal.ZERO;
        return amount.multiply(rate).divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
