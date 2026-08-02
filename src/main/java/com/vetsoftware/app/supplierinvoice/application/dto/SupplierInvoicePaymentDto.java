package com.vetsoftware.app.supplierinvoice.application.dto;

import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePayment;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SupplierInvoicePaymentDto(
    Long id,
    BigDecimal amount,
    LocalDate paymentDate,
    SupplierInvoicePaymentMethod method,
    String reference,
    String note,
    LocalDateTime createdDate,
    Long createdBy) {
  public static SupplierInvoicePaymentDto from(SupplierInvoicePayment p) {
    return new SupplierInvoicePaymentDto(
        p.getId(),
        p.getAmount(),
        p.getPaymentDate(),
        p.getMethod(),
        p.getReference(),
        p.getNote(),
        p.getCreatedDate(),
        p.getCreatedBy());
  }
}
