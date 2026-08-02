package com.vetsoftware.app.goodsreceipt.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Línea de entrada para crear/actualizar una recepción. {@code purchaseOrderLineId} es opcional
 * (recepción parcial).
 */
public record GoodsReceiptLineCommand(
    Long productId,
    Long purchaseOrderLineId,
    String lotNumber,
    LocalDate expireDate,
    int quantityReceived,
    BigDecimal unitCost) {}
