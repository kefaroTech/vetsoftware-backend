package com.vetsoftware.app.electronicdocument.domain;

import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;

/**
 * Calcula las retenciones que el adquiriente (agente retenedor) practica sobre
 * la venta, usando las tarifas configuradas por el emisor (WithholdingConfig).
 * ReteFuente y ReteICA aplican sobre la base gravable; ReteIVA aplica sobre el
 * IVA generado. Devuelve {@link WithholdingAmounts#NONE} si el adquiriente no
 * es agente retenedor (las tarifas en cero también producen cero).
 *
 * <p>
 * B3 — cuantías mínimas: la reteFuente y la reteIVA no se practican bajo una
 * base mínima (E.T. arts. 401/392). Se usa el umbral general de servicios (4
 * UVT); no se distingue por concepto (compras 27 UVT) por no llevar el concepto
 * en la línea — es una aproximación conservadora documentada. Si no hay UVT
 * disponible no se aplica mínimo (no se bloquea la retención). B7 — la reteICA
 * se expresa en ‰ (por mil), no en %.
 */
public final class WithholdingCalculator {
    /**
     * Base gravable mínima (en UVT) para practicar reteFuente/reteIVA (umbral
     * general de servicios).
     */
    private static final BigDecimal RETENTION_MIN_UVT = BigDecimal.valueOf(4);

    private WithholdingCalculator() {
    }

    public static WithholdingAmounts compute(boolean withholdingAgent, BigDecimal taxableBase,
            BigDecimal ivaAmount, BigDecimal reteFuenteRate, BigDecimal reteIvaRate,
            BigDecimal reteIcaRate, BigDecimal uvt) {
        if (!withholdingAgent)
            return WithholdingAmounts.NONE;
        // reteFuente/reteIVA: solo si la base supera la cuantía mínima (evita
        // sobre-retención en montos
        // chicos).
        boolean meetsMinimum = meetsMinimum(taxableBase, uvt);
        BigDecimal reteFuente = meetsMinimum
                ? Money.percentOf(taxableBase, reteFuenteRate)
                : BigDecimal.ZERO;
        BigDecimal reteIva = meetsMinimum
                ? Money.percentOf(ivaAmount, reteIvaRate)
                : BigDecimal.ZERO;
        // reteICA: tarifa en ‰ (por mil), sin mínimo en UVT (los mínimos de ICA son
        // municipales y
        // varían).
        BigDecimal reteIca = Money.perMilOf(taxableBase, reteIcaRate);
        return new WithholdingAmounts(reteFuente, reteIva, reteIca);
    }

    private static boolean meetsMinimum(BigDecimal taxableBase, BigDecimal uvt) {
        if (uvt == null || uvt.signum() <= 0)
            return true; // sin UVT no se puede evaluar el mínimo: no bloquear
        return taxableBase.compareTo(RETENTION_MIN_UVT.multiply(uvt)) >= 0;
    }
}
