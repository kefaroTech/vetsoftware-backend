package com.vetsoftware.app.goodsreceipt.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Afecta el inventario (feature {@code inventory}) al confirmar/cancelar una recepción de
 * mercancía. Cada línea confirmada registra una ENTRADA de kardex por sede (lote + costo real). El
 * adapter de orquestación traduce a {@code StockLedgerUseCase} con referencia {@code GOODS_RECEIPT}
 * y el id de la recepción (idempotencia + compensación). {@code recordPurchase} NO es idempotente:
 * la guarda es la transición de estado DRAFT→CONFIRMED.
 */
public interface InventoryLedgerPort {

  /** Registra la entrada de inventario de una línea de la recepción. */
  void recordReceipt(
      Long companyId,
      Long branchId,
      Long productId,
      String lotNumber,
      LocalDate expireDate,
      int quantity,
      BigDecimal unitCost,
      Long receiptId,
      Long createdBy);

  /**
   * Compensa (revierte) todas las entradas registradas por la recepción al cancelarla. Idempotente.
   */
  void reverseReceipt(Long receiptId, Long actorId);
}
