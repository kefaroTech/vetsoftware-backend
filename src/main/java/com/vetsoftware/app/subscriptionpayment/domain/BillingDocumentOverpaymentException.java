package com.vetsoftware.app.subscriptionpayment.domain;

import java.math.BigDecimal;

/**
 * La suma de los orígenes que saldan de inmediato ({@code CREDIT_NOTE},
 * {@code WITHHOLDING}, {@code CUSTOMER_CREDIT}, {@code ROUNDING},
 * {@code WRITE_OFF}) no puede superar el total del documento cuando ya venía
 * cobrado por otra vía. R3 acota cada origen por sí mismo, pero nunca la suma
 * de varios orígenes contra el mismo documento.
 */
public class BillingDocumentOverpaymentException extends RuntimeException {
    private final Long documentId;
    private final BigDecimal excess;

    public BillingDocumentOverpaymentException(Long documentId, BigDecimal excess) {
        super("Applying this source to document " + documentId + " exceeds its total by " + excess);
        this.documentId = documentId;
        this.excess = excess;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public BigDecimal getExcess() {
        return excess;
    }
}
