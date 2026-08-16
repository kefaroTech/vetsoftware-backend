package com.vetsoftware.app.goodsreceipt.infrastructure.inventory;

import com.vetsoftware.app.goodsreceipt.application.port.out.InventoryLedgerPort;
import com.vetsoftware.app.inventory.application.command.RecordPurchaseCommand;
import com.vetsoftware.app.inventory.application.port.in.StockLedgerUseCase;
import com.vetsoftware.app.inventory.domain.StockReferenceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Adapter de orquestación recepción → inventario. Es el ÚNICO punto de esta
 * feature que conoce el {@code StockLedgerUseCase} de {@code inventory};
 * traduce la entrada/reversión a movimientos de kardex con referencia
 * {@link StockReferenceType#GOODS_RECEIPT} y el id de la recepción
 * (trazabilidad + compensación).
 */
@Component("goodsReceiptLedgerInventoryAdapter")
public class LedgerInventoryAdapter implements InventoryLedgerPort {

    private final StockLedgerUseCase stockLedger;

    public LedgerInventoryAdapter(StockLedgerUseCase stockLedger) {
        this.stockLedger = stockLedger;
    }

    @Override
    public void recordReceipt(Long companyId, Long branchId, Long productId, String lotNumber,
            LocalDate expireDate, int quantity, BigDecimal unitCost, Long receiptId,
            Long createdBy) {
        stockLedger.recordPurchase(new RecordPurchaseCommand(companyId, branchId, productId,
                lotNumber, expireDate, quantity, unitCost, StockReferenceType.GOODS_RECEIPT,
                receiptId, createdBy));
    }

    @Override
    public void reverseReceipt(Long receiptId, Long companyId, Long actorId) {
        stockLedger.reverse(StockReferenceType.GOODS_RECEIPT, receiptId, companyId, actorId);
    }
}
