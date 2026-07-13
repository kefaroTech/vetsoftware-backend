package com.vetsoftware.app.productchargeopenaccount.application.port.out;

/**
 * Afecta el inventario (feature {@code inventory}) al cargar/anular una venta de cuenta abierta. Reemplaza al viejo
 * descuento plano sobre {@code products}: ahora la salida se registra en el kardex por sede (FEFO + costo por lote),
 * con idempotencia y compensación por la referencia del cargo. El adapter de orquestación traduce a
 * {@code StockLedgerUseCase}.
 */
public interface InventoryLedgerPort {

    /**
     * Registra la salida de inventario por el cargo (idempotente por el id del cargo). Puede lanzar
     * {@code InsufficientStockException} si no hay existencias y la empresa no permite stock negativo.
     */
    void recordSale(Long companyId, Long branchId, Long productId, int quantity, Long chargeId, Long createdBy);

    /** Compensa (repone) la salida registrada por el cargo al anularlo. Idempotente. */
    void reverseSale(Long chargeId, Long actorId);
}
