package com.vetsoftware.app.electronicdocument.domain;

import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;

/**
 * Calcula las retenciones que el adquiriente (agente retenedor) practica sobre la venta, usando las tarifas
 * configuradas por el emisor (WithholdingConfig). ReteFuente y ReteICA aplican sobre la base gravable;
 * ReteIVA aplica sobre el IVA generado. Devuelve {@link WithholdingAmounts#NONE} si el adquiriente no es
 * agente retenedor (las tarifas en cero también producen cero).
 */
public final class WithholdingCalculator {
    private WithholdingCalculator() {}

    public static WithholdingAmounts compute(boolean withholdingAgent, BigDecimal taxableBase,
                                             BigDecimal ivaAmount, BigDecimal reteFuenteRate,
                                             BigDecimal reteIvaRate, BigDecimal reteIcaRate) {
        if (!withholdingAgent) return WithholdingAmounts.NONE;
        return new WithholdingAmounts(
                Money.percentOf(taxableBase, reteFuenteRate),
                Money.percentOf(ivaAmount, reteIvaRate),
                Money.percentOf(taxableBase, reteIcaRate));
    }
}
