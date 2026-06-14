package com.vetsoftware.app.electronicdocument.domain;

import java.math.BigDecimal;

/** Pago del documento: medio codificado DIAN + monto. Congelado al emitir. */
public class ElectronicDocumentPayment {
    private Long id;
    private final PaymentMeans paymentMeans;
    private final BigDecimal amount;

    public ElectronicDocumentPayment(Long id, PaymentMeans paymentMeans, BigDecimal amount) {
        if (paymentMeans == null) throw new IllegalArgumentException("paymentMeans is required");
        if (amount == null) throw new IllegalArgumentException("payment amount is required");
        this.id = id;
        this.paymentMeans = paymentMeans;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public PaymentMeans getPaymentMeans() { return paymentMeans; }
    public BigDecimal getAmount() { return amount; }
}
