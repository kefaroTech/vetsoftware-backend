package com.vetsoftware.app.goodsreceipt.infrastructure.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GoodsReceiptLineRequest(@NotNull Long productId, Long purchaseOrderLineId,
        @Size(max = 60) String lotNumber, LocalDate expireDate, @Positive int quantityReceived,
        @NotNull @DecimalMin("0.0") BigDecimal unitCost) {
}
