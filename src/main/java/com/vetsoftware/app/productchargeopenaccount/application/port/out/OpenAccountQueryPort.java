package com.vetsoftware.app.productchargeopenaccount.application.port.out;

import com.vetsoftware.app.productchargeopenaccount.domain.OpenAccountRef;
import java.math.BigDecimal;
import java.util.Optional;

public interface OpenAccountQueryPort {
  Optional<OpenAccountRef> findById(Long openAccountId);

  /**
   * Toma el bloqueo pesimista (FOR UPDATE) sobre la cuenta al inicio de la operación, para
   * serializar cargos/abonos concurrentes desde el read-modify-write completo (validación de estado
   * + recálculo) y no solo durante el recálculo. Debe invocarse como primera sentencia del caso de
   * uso. No-op si la cuenta no existe (la validación posterior lo reporta).
   */
  void lockForUpdate(Long openAccountId);

  boolean isOpen(Long openAccountId);

  /** Saldo pendiente actual de la cuenta (total - abonos). ZERO si no existe. */
  BigDecimal outstandingAmount(Long openAccountId);
}
