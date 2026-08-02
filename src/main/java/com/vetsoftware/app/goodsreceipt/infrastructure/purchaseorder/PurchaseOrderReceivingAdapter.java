package com.vetsoftware.app.goodsreceipt.infrastructure.purchaseorder;

import com.vetsoftware.app.goodsreceipt.application.port.out.PurchaseOrderReceivingPort;
import com.vetsoftware.app.goodsreceipt.application.port.out.ReceivedLine;
import com.vetsoftware.app.purchaseorder.application.command.ApplyReceiptCommand;
import com.vetsoftware.app.purchaseorder.application.command.ReceivedPurchaseOrderLine;
import com.vetsoftware.app.purchaseorder.application.port.in.ReceivePurchaseOrderUseCase;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Adapter de orquestación recepción → orden de compra. Es el ÚNICO punto de esta feature que conoce
 * el {@code ReceivePurchaseOrderUseCase} de {@code purchaseorder}; traduce las cantidades
 * recibidas/revertidas a {@link ApplyReceiptCommand} + {@link ReceivedPurchaseOrderLine} para
 * avanzar/retroceder el estado de recepción de la orden.
 */
@Component("goodsReceiptPurchaseOrderReceivingAdapter")
public class PurchaseOrderReceivingAdapter implements PurchaseOrderReceivingPort {

  private final ReceivePurchaseOrderUseCase receivePurchaseOrder;

  public PurchaseOrderReceivingAdapter(ReceivePurchaseOrderUseCase receivePurchaseOrder) {
    this.receivePurchaseOrder = receivePurchaseOrder;
  }

  @Override
  public void applyReceipt(
      Long purchaseOrderId, Long companyId, List<ReceivedLine> lines, Long actorId) {
    receivePurchaseOrder.applyReceipt(
        new ApplyReceiptCommand(purchaseOrderId, companyId, mapLines(lines), actorId));
  }

  @Override
  public void revertReceipt(
      Long purchaseOrderId, Long companyId, List<ReceivedLine> lines, Long actorId) {
    receivePurchaseOrder.revertReceipt(
        new ApplyReceiptCommand(purchaseOrderId, companyId, mapLines(lines), actorId));
  }

  private List<ReceivedPurchaseOrderLine> mapLines(List<ReceivedLine> lines) {
    return lines.stream()
        .map(line -> new ReceivedPurchaseOrderLine(line.purchaseOrderLineId(), line.quantity()))
        .toList();
  }
}
