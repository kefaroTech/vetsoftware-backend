package com.vetsoftware.app.supplierinvoice.infrastructure.web.response;

import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SupplierInvoicePaymentResponse(
        Long id,
        BigDecimal amount,
        LocalDate paymentDate,
        SupplierInvoicePaymentMethod method,
        String reference,
        String note,
        LocalDateTime createdDate,
        Long createdBy
) {}
