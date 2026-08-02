package com.vetsoftware.app.inventory.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Renglón del libro de compras: una entrada de mercancía (movimiento PURCHASE) con su costo. */
public record PurchaseView(
    Long id,
    Long productId,
    String productName,
    String productCode,
    Long lotId,
    Long branchId,
    String branchName,
    int quantity,
    BigDecimal unitCost,
    BigDecimal total,
    Long referenceId,
    LocalDateTime createdDate) {}
