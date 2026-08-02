package com.vetsoftware.app.inventory.application.port.out;

import com.vetsoftware.app.inventory.domain.StockMovement;
import com.vetsoftware.app.inventory.domain.StockReferenceType;
import java.util.List;

public interface StockMovementRepository {
  StockMovement save(StockMovement movement);

  /** ¿Ya hay movimientos para esta referencia? (idempotencia de recordSale). */
  boolean existsByReference(StockReferenceType referenceType, Long referenceId);

  /** Movimientos de una referencia (para compensar/anular). */
  List<StockMovement> findByReference(StockReferenceType referenceType, Long referenceId);
}
