package com.vetsoftware.app.cashregister.application.command;

import com.vetsoftware.app.cashregister.domain.CashReferenceType;
import java.util.List;

/**
 * Compensar (VOID_OUT) en la caja OPEN actual los ingresos de una venta/abono anulado. Los {@code payments} son los
 * montos originales por método (los conoce quien anula: el documento/abono). Idempotente por (referenceType,
 * referenceId, método). Si no hay caja abierta, es no-op (no se puede compensar contra un cajón ya cerrado/arqueado).
 */
public record ReverseCashMovementsCommand(Long companyId, Long branchId, String terminal,
                                          CashReferenceType referenceType, Long referenceId,
                                          List<CashPaymentLine> payments, Long employeeId) {}
