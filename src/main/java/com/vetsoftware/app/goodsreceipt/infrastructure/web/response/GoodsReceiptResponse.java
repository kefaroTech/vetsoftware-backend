package com.vetsoftware.app.goodsreceipt.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record GoodsReceiptResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BranchSummary branch,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SupplierSummary supplier,
        Long purchaseOrderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate receiptDate,
        String supplierInvoiceNumber, String notes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) GoodsReceiptStatus status,
        List<GoodsReceiptLineResponse> lines,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        Long createdBy, LocalDateTime updatedDate, Long updatedBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
