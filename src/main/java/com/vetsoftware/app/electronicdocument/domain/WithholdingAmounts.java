package com.vetsoftware.app.electronicdocument.domain;

import java.math.BigDecimal;

/**
 * Montos de retención practicados por el adquiriente (agente retenedor) sobre
 * la venta, congelados en el documento. Cero cuando el adquiriente no es agente
 * retenedor. Neto a pagar = total − total de retenciones.
 */
public record WithholdingAmounts(BigDecimal reteFuente, BigDecimal reteIva, BigDecimal reteIca) {

    public static final WithholdingAmounts NONE = new WithholdingAmounts(BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO);

    public WithholdingAmounts {
        reteFuente = nz(reteFuente);
        reteIva = nz(reteIva);
        reteIca = nz(reteIca);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public BigDecimal total() {
        return reteFuente.add(reteIva).add(reteIca);
    }
}
