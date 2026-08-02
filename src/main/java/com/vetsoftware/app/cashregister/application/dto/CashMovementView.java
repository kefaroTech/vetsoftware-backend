package com.vetsoftware.app.cashregister.application.dto;

import com.vetsoftware.app.cashregister.domain.CashMovement;
import com.vetsoftware.app.cashregister.domain.CashMovementType;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashReferenceType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Salida de un movimiento de caja (el {@code amount} es positivo; el signo lo da el tipo). */
public record CashMovementView(
    Long id,
    CashMovementType type,
    CashPaymentMethod method,
    BigDecimal amount,
    CashReferenceType referenceType,
    Long referenceId,
    Long createdByEmployeeId,
    LocalDateTime createdAt,
    String note) {

  public static CashMovementView from(CashMovement m) {
    return new CashMovementView(
        m.getId(),
        m.getType(),
        m.getMethod(),
        m.getAmount(),
        m.getReferenceType(),
        m.getReferenceId(),
        m.getCreatedByEmployeeId(),
        m.getCreatedAt(),
        m.getNote());
  }
}
