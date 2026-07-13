package com.vetsoftware.app.supplierinvoice.infrastructure.web.request;

import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterSupplierPaymentRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        @NotNull LocalDate paymentDate,
        @NotNull SupplierInvoicePaymentMethod method,
        @Size(max = 80) String reference,
        @Size(max = 300) String note,
        @NotNull Long version
) {}
