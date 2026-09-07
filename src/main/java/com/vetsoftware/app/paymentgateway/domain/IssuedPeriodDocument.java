package com.vetsoftware.app.paymentgateway.domain;

import java.math.BigDecimal;

/**
 * @param balanceAmount
 *            lo que de verdad queda por cobrar tras cualquier abono ya aplicado
 *            (p. ej. saldo a favor por prorrateo). El primer cobro carga este
 *            importe, nunca {@code totalAmount}.
 */
public record IssuedPeriodDocument(Long documentId, String documentNumber, BigDecimal totalAmount,
        BigDecimal balanceAmount, String currency, boolean issued) {
}
