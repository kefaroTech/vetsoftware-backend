package com.vetsoftware.app.purchaseorder.application.command;

import java.math.BigDecimal;

/**
 * Datos de una línea de producto en la creación/edición de una orden de compra.
 */
public record PurchaseOrderLineCommand(Long productId, int quantityOrdered, BigDecimal unitCost) {
}
