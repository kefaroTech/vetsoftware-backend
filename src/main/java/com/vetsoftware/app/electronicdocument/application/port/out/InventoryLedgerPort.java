package com.vetsoftware.app.electronicdocument.application.port.out;

/**
 * Afecta el inventario (feature {@code inventory}) por una venta POS directa. SOLO para documentos
 * POS (sin cuenta abierta): las ventas por cuenta ya descontaron al crear cada cargo. La salida se
 * registra en el kardex con referencia {@code POS_DOCUMENT} + id del documento (idempotente +
 * compensable). El adapter de orquestación traduce a {@code StockLedgerUseCase}.
 */
public interface InventoryLedgerPort {

  /**
   * Registra la salida de inventario por una línea de producto del POS. Idempotente por el id del
   * documento. La venta de mostrador NUNCA se frena por stock: siempre permite negativo (no
   * consulta el flag de empresa).
   */
  void recordPosSale(
      Long companyId, Long branchId, Long productId, int quantity, Long documentId, Long issuedBy);

  /**
   * Compensa (repone) las salidas registradas por el documento POS al reversarlo (nota crédito
   * total). Idempotente.
   */
  void reversePosSale(Long documentId, Long actorId);
}
