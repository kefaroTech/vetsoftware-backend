package com.vetsoftware.app.supplierinvoice.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SupplierInvoicePaymentResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal amount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate paymentDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SupplierInvoicePaymentMethod method,
        String reference, String note,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        Long createdBy) {
}
