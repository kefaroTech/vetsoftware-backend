package com.vetsoftware.app.goodsreceipt.infrastructure.web.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoodsReceiptLineResponse(
        Long id,
        ProductSummary product,
        Long purchaseOrderLineId,
        String lotNumber,
        LocalDate expireDate,
        int quantityReceived,
        BigDecimal unitCost
) {}
