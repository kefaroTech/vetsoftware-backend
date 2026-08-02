package com.vetsoftware.app.supplierinvoice.application.command;

import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterSupplierPaymentCommand(
    Long invoiceId,
    BigDecimal amount,
    LocalDate paymentDate,
    SupplierInvoicePaymentMethod method,
    String reference,
    String note,
    Long companyId,
    Long actorId,
    Long version) {}
