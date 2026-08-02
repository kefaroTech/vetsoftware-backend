package com.vetsoftware.app.goodsreceipt.application.dto;

import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptLine;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GoodsReceiptLineDto(
    Long id,
    ProductSummaryDto product,
    Long purchaseOrderLineId,
    String lotNumber,
    LocalDate expireDate,
    int quantityReceived,
    BigDecimal unitCost) {
  public static GoodsReceiptLineDto from(GoodsReceiptLine line) {
    return new GoodsReceiptLineDto(
        line.getId(),
        ProductSummaryDto.from(line.getProduct()),
        line.getPurchaseOrderLineId(),
        line.getLotNumber(),
        line.getExpireDate(),
        line.getQuantityReceived(),
        line.getUnitCost());
  }
}
