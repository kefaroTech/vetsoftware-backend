package com.vetsoftware.app.productchargeopenaccount.infrastructure.inventory;

import com.vetsoftware.app.inventory.application.command.RecordSaleCommand;
import com.vetsoftware.app.inventory.application.port.in.StockLedgerUseCase;
import com.vetsoftware.app.inventory.domain.StockReferenceType;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.InventoryLedgerPort;
import org.springframework.stereotype.Component;

/**
 * Adapter de orquestación cuenta-abierta → inventario. Es el ÚNICO punto de
 * esta feature que conoce el {@code StockLedgerUseCase} de {@code inventory};
 * traduce el cargo/anulación a movimientos de kardex con referencia
 * {@link StockReferenceType#OPEN_ACCOUNT_CHARGE} y el id del cargo
 * (idempotencia + compensación).
 */
@Component("openAccountLedgerInventoryAdapter")
public class LedgerInventoryAdapter implements InventoryLedgerPort {

    private final StockLedgerUseCase stockLedger;

    public LedgerInventoryAdapter(StockLedgerUseCase stockLedger) {
        this.stockLedger = stockLedger;
    }

    @Override
    public void recordSale(Long companyId, Long branchId, Long productId, int quantity,
            Long chargeId, Long createdBy) {
        // allowNegative=false: la cuenta abierta respeta el flag de stock negativo de
        // la empresa.
        stockLedger.recordSale(new RecordSaleCommand(companyId, branchId, productId, quantity,
                StockReferenceType.OPEN_ACCOUNT_CHARGE, chargeId, createdBy, false));
    }

    @Override
    public void reverseSale(Long chargeId, Long companyId, Long actorId) {
        stockLedger.reverse(StockReferenceType.OPEN_ACCOUNT_CHARGE, chargeId, companyId, actorId);
    }
}
