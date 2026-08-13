package com.vetsoftware.app.supplierinvoice.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SupplierInvoiceResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BranchSummary branch,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SupplierSummary supplier,
        Long purchaseOrderId, Long goodsReceiptId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String invoiceNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate issueDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate dueDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal subtotal,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal taxAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal withholdingAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal total,
        BigDecimal payableAmount, BigDecimal paidAmount, BigDecimal balance,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SupplierInvoiceStatus status,
        String notes, List<SupplierInvoicePaymentResponse> payments,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        Long createdBy, LocalDateTime updatedDate, Long updatedBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
