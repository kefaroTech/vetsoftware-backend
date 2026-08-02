package com.vetsoftware.app.cashregister.application.command;

import com.vetsoftware.app.cashregister.domain.CashMovementType;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import java.math.BigDecimal;

/**
 * Registrar un movimiento manual en una sesión de caja abierta. Desde el REST solo se aceptan tipos
 * manuales (MANUAL_IN/WITHDRAWAL/EXPENSE); los movimientos de venta/abono/reversa los inyecta la
 * orquestación.
 */
public record RegisterCashMovementCommand(
    Long companyId,
    Long sessionId,
    CashMovementType type,
    CashPaymentMethod method,
    BigDecimal amount,
    Long createdByEmployeeId,
    String note) {}
