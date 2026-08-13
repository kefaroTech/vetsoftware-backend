package com.vetsoftware.app.goodsreceipt.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoodsReceiptLineResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProductSummary product,
        Long purchaseOrderLineId, String lotNumber, LocalDate expireDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int quantityReceived,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal unitCost) {
}
