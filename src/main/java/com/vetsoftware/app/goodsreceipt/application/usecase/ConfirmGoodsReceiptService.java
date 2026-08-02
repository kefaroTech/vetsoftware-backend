package com.vetsoftware.app.goodsreceipt.application.usecase;

import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import com.vetsoftware.app.goodsreceipt.application.port.in.ConfirmGoodsReceiptUseCase;
import com.vetsoftware.app.goodsreceipt.application.port.out.GoodsReceiptRepository;
import com.vetsoftware.app.goodsreceipt.application.port.out.InventoryLedgerPort;
import com.vetsoftware.app.goodsreceipt.application.port.out.PurchaseOrderReceivingPort;
import com.vetsoftware.app.goodsreceipt.application.port.out.ReceivedLine;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceipt;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptLine;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptNotFoundException;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptStatus;
import com.vetsoftware.app.goodsreceipt.domain.InvalidGoodsReceiptStatusTransitionException;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirma una recepción DRAFT: registra la ENTRADA de inventario por cada
 * línea y, si viene de una orden de compra, aplica lo recibido en ella; luego
 * marca CONFIRMED. La transición DRAFT→CONFIRMED es la guarda de idempotencia
 * porque {@code recordReceipt} NO es idempotente.
 */
@Observed(name = "goods.receipt.confirm")
@Service
public class ConfirmGoodsReceiptService implements ConfirmGoodsReceiptUseCase {
    private final GoodsReceiptRepository repository;
    private final InventoryLedgerPort inventoryLedger;
    private final PurchaseOrderReceivingPort purchaseOrderReceiving;

    public ConfirmGoodsReceiptService(GoodsReceiptRepository repository,
            InventoryLedgerPort inventoryLedger,
            PurchaseOrderReceivingPort purchaseOrderReceiving) {
        this.repository = repository;
        this.inventoryLedger = inventoryLedger;
        this.purchaseOrderReceiving = purchaseOrderReceiving;
    }

    @Override
    @Transactional
    public GoodsReceiptDto execute(Long id, Long companyId, Long actorId) {
        GoodsReceipt receipt = repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new GoodsReceiptNotFoundException(id));
        if (receipt.getStatus() != GoodsReceiptStatus.DRAFT) {
            throw new InvalidGoodsReceiptStatusTransitionException(receipt.getStatus(),
                    GoodsReceiptStatus.CONFIRMED);
        }

        for (GoodsReceiptLine line : receipt.getLines()) {
            inventoryLedger.recordReceipt(companyId, receipt.getBranch().id(),
                    line.getProduct().id(), line.getLotNumber(), line.getExpireDate(),
                    line.getQuantityReceived(), line.getUnitCost(), receipt.getId(), actorId);
        }

        if (receipt.getPurchaseOrderId() != null) {
            List<ReceivedLine> receivedLines = receipt.getLines().stream()
                    .filter(line -> line.getPurchaseOrderLineId() != null)
                    .map(line -> new ReceivedLine(line.getPurchaseOrderLineId(),
                            line.getQuantityReceived()))
                    .toList();
            purchaseOrderReceiving.applyReceipt(receipt.getPurchaseOrderId(), companyId,
                    receivedLines, actorId);
        }

        receipt.confirm(actorId);
        return GoodsReceiptDto.from(repository.save(receipt));
    }
}
