package com.vetsoftware.app.electronicdocument.infrastructure.inventory;

import com.vetsoftware.app.electronicdocument.application.port.out.InventoryLedgerPort;
import com.vetsoftware.app.inventory.application.command.RecordSaleCommand;
import com.vetsoftware.app.inventory.application.port.in.StockLedgerUseCase;
import com.vetsoftware.app.inventory.domain.StockReferenceType;
import org.springframework.stereotype.Component;

/**
 * Adapter de orquestación POS → inventario. Es el ÚNICO punto de esta feature que conoce el
 * {@code StockLedgerUseCase} de {@code inventory}; traduce la venta/anulación POS a movimientos de kardex con
 * referencia {@link StockReferenceType#POS_DOCUMENT} y el id del documento (idempotencia + compensación).
 */
@Component("posLedgerInventoryAdapter")
public class LedgerInventoryAdapter implements InventoryLedgerPort {

    private final StockLedgerUseCase stockLedger;

    public LedgerInventoryAdapter(StockLedgerUseCase stockLedger) {
        this.stockLedger = stockLedger;
    }

    @Override
    public void recordPosSale(Long companyId, Long branchId, Long productId, int quantity, Long documentId,
                              Long issuedBy) {
        // allowNegative=true: la venta de mostrador nunca se frena por stock.
        stockLedger.recordSale(new RecordSaleCommand(companyId, branchId, productId, quantity,
            StockReferenceType.POS_DOCUMENT, documentId, issuedBy, true));
    }

    @Override
    public void reversePosSale(Long documentId, Long actorId) {
        stockLedger.reverse(StockReferenceType.POS_DOCUMENT, documentId, actorId);
    }
}
