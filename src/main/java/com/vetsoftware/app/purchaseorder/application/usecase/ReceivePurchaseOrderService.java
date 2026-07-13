package com.vetsoftware.app.purchaseorder.application.usecase;

import com.vetsoftware.app.purchaseorder.application.command.ApplyReceiptCommand;
import com.vetsoftware.app.purchaseorder.application.command.ReceivedPurchaseOrderLine;
import com.vetsoftware.app.purchaseorder.application.port.in.ReceivePurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.application.port.out.PurchaseOrderRepository;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrder;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio INTERNO (sin {@code @PreAuthorize}) que aplica/revierte recepciones de mercancía sobre una orden de
 * compra. Lo invoca la feature {@code goodsreceipt} dentro de su transacción ya autorizada — igual que el ledger
 * de inventario. No se expone por REST.
 */
@Observed(name = "purchaseOrder.receive")
@Service
public class ReceivePurchaseOrderService implements ReceivePurchaseOrderUseCase {
    private final PurchaseOrderRepository repository;

    public ReceivePurchaseOrderService(PurchaseOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void applyReceipt(ApplyReceiptCommand command) {
        PurchaseOrder order = repository.findByIdAndCompanyId(command.purchaseOrderId(), command.companyId())
            .orElseThrow(() -> new PurchaseOrderNotFoundException(command.purchaseOrderId()));
        for (ReceivedPurchaseOrderLine line : command.lines()) {
            order.receiveLine(line.purchaseOrderLineId(), line.quantity());
        }
        order.recalculateStatusAfterReceipt(command.actorId());
        repository.save(order);
    }

    @Override
    @Transactional
    public void revertReceipt(ApplyReceiptCommand command) {
        PurchaseOrder order = repository.findByIdAndCompanyId(command.purchaseOrderId(), command.companyId())
            .orElseThrow(() -> new PurchaseOrderNotFoundException(command.purchaseOrderId()));
        for (ReceivedPurchaseOrderLine line : command.lines()) {
            order.revertLine(line.purchaseOrderLineId(), line.quantity());
        }
        order.recalculateStatusAfterRevert(command.actorId());
        repository.save(order);
    }
}
