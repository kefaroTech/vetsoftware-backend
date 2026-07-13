package com.vetsoftware.app.cashregister.application.port.in;

import com.vetsoftware.app.cashregister.application.command.RegisterCashInflowCommand;
import com.vetsoftware.app.cashregister.application.command.ReverseCashMovementsCommand;

/**
 * Puerto INTERNO de orquestación: lo consumen POS y cuenta abierta (vía sus adapters) para afectar la caja sin
 * importar el dominio de otras features. Sin {@code @PreAuthorize}: corre dentro de la transacción del caller, que ya
 * validó ownership/permiso de la acción de negocio (venta/abono), igual que {@code StockLedgerUseCase} en inventario.
 */
public interface CashLedgerUseCase {

    /** Registra el ingreso (SALE_IN / OPEN_ACCOUNT_IN según referencia) en la caja OPEN de la sede. Idempotente. */
    void registerInflow(RegisterCashInflowCommand command);

    /** Compensa con VOID_OUT los ingresos de una venta/abono anulado en la caja OPEN actual. Idempotente. */
    void reverse(ReverseCashMovementsCommand command);

    /**
     * Bloqueo "caja requerida" (F4): si la empresa exige caja ({@code cashregister.required}, default true) y no hay
     * sesión OPEN en la sede, lanza {@code NoOpenCashSessionException} (→ 409) ANTES de emitir/persistir el cobro. Si
     * la empresa no la exige, es no-op. Lo invocan POS y cuenta abierta al inicio del cobro.
     */
    void ensureCashAvailable(Long companyId, Long branchId, String terminal);
}
