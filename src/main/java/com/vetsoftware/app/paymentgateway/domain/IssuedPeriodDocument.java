package com.vetsoftware.app.paymentgateway.domain;

import java.math.BigDecimal;

/** El documento de cobro emitido, visto desde aquí. */
public record IssuedPeriodDocument(Long documentId, String documentNumber, BigDecimal totalAmount,
        String currency, boolean issued) {
}
