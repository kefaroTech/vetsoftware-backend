package com.vetsoftware.app.goodsreceipt.application.usecase;

import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import com.vetsoftware.app.goodsreceipt.application.port.in.CancelGoodsReceiptUseCase;
import com.vetsoftware.app.goodsreceipt.application.port.out.GoodsReceiptRepository;
import com.vetsoftware.app.goodsreceipt.application.port.out.InventoryLedgerPort;
import com.vetsoftware.app.goodsreceipt.application.port.out.PurchaseOrderReceivingPort;
import com.vetsoftware.app.goodsreceipt.application.port.out.ReceivedLine;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceipt;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptNotFoundException;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptStatus;
import com.vetsoftware.app.goodsreceipt.domain.InvalidGoodsReceiptStatusTransitionException;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "goods.receipt.cancel")
@Service
public class CancelGoodsReceiptService implements CancelGoodsReceiptUseCase {
    private final GoodsReceiptRepository repository;
    private final InventoryLedgerPort inventoryLedger;
    private final PurchaseOrderReceivingPort purchaseOrderReceiving;

    public CancelGoodsReceiptService(GoodsReceiptRepository repository,
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
        if (receipt.getStatus() != GoodsReceiptStatus.CONFIRMED) {
            throw new InvalidGoodsReceiptStatusTransitionException(receipt.getStatus(),
                    GoodsReceiptStatus.CANCELLED);
        }

        inventoryLedger.reverseReceipt(receipt.getId(), companyId, actorId);

        if (receipt.getPurchaseOrderId() != null) {
            List<ReceivedLine> receivedLines = receipt.getLines().stream()
                    .filter(line -> line.getPurchaseOrderLineId() != null)
                    .map(line -> new ReceivedLine(line.getPurchaseOrderLineId(),
                            line.getQuantityReceived()))
                    .toList();
            purchaseOrderReceiving.revertReceipt(receipt.getPurchaseOrderId(), companyId,
                    receivedLines, actorId);
        }

        receipt.cancel(actorId);
        return GoodsReceiptDto.from(repository.save(receipt));
    }
}
