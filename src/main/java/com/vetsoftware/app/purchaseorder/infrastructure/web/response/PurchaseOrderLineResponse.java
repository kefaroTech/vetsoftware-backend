package com.vetsoftware.app.purchaseorder.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record PurchaseOrderLineResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProductSummary product,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int quantityOrdered,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal unitCost,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int quantityReceived,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int pendingQuantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean fullyReceived) {
}
