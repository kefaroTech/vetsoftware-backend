package com.vetsoftware.app.supplierinvoice.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Abono (pago total o parcial) aplicado a una factura de proveedor.
 * Append-only: nunca se edita ni borra.
 */
public class SupplierInvoicePayment {
    private final Long id;
    private final BigDecimal amount;
    private final LocalDate paymentDate;
    private final SupplierInvoicePaymentMethod method;
    private final String reference;
    private final String note;
    private final LocalDateTime createdDate;
    private final Long createdBy;

    public SupplierInvoicePayment(Long id, BigDecimal amount, LocalDate paymentDate,
            SupplierInvoicePaymentMethod method, String reference, String note,
            LocalDateTime createdDate, Long createdBy) {
        if (amount == null || amount.signum() <= 0)
            throw new IllegalArgumentException("payment amount must be positive");
        if (paymentDate == null)
            throw new IllegalArgumentException("paymentDate is required");
        if (method == null)
            throw new IllegalArgumentException("payment method is required");
        if (reference != null && reference.length() > 80)
            throw new IllegalArgumentException("reference must be 80 chars or less");
        if (note != null && note.length() > 300)
            throw new IllegalArgumentException("note must be 300 chars or less");
        this.id = id;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.method = method;
        this.reference = reference;
        this.note = note;
        this.createdDate = createdDate;
        this.createdBy = createdBy;
    }

    public static SupplierInvoicePayment create(BigDecimal amount, LocalDate paymentDate,
            SupplierInvoicePaymentMethod method, String reference, String note, Long createdBy) {
        return new SupplierInvoicePayment(null, amount, paymentDate, method, reference, note,
                LocalDateTime.now(), createdBy);
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public SupplierInvoicePaymentMethod getMethod() {
        return method;
    }

    public String getReference() {
        return reference;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getCreatedBy() {
        return createdBy;
    }
}
