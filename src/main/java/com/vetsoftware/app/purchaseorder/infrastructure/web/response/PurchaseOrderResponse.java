package com.vetsoftware.app.purchaseorder.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BranchSummary branch,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SupplierSummary supplier,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PurchaseOrderStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate orderDate,
        LocalDate expectedDate, String notes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<PurchaseOrderLineResponse> lines,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        Long createdBy, LocalDateTime updatedDate, Long updatedBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
