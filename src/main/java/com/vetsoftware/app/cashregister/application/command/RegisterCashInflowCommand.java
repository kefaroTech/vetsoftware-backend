package com.vetsoftware.app.cashregister.application.command;

import com.vetsoftware.app.cashregister.domain.CashReferenceType;
import java.util.List;

/**
 * Registrar el ingreso de una venta POS / abono en la caja OPEN de una sede. El tipo (SALE_IN / OPEN_ACCOUNT_IN) lo
 * deriva el service del {@code referenceType}. Idempotente por (referenceType, referenceId, método).
 */
public record RegisterCashInflowCommand(Long companyId, Long branchId, String terminal,
                                        CashReferenceType referenceType, Long referenceId,
                                        List<CashPaymentLine> payments, Long employeeId) {}
