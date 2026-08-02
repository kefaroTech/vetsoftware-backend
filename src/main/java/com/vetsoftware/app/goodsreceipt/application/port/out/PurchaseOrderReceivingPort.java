package com.vetsoftware.app.goodsreceipt.application.port.out;

import java.util.List;

/**
 * Registra/revierte lo recibido contra una orden de compra (feature {@code purchaseorder}) cuando
 * la recepción viene de una orden. El adapter de orquestación traduce a {@code
 * ReceivePurchaseOrderUseCase}.
 */
public interface PurchaseOrderReceivingPort {

  /**
   * Aplica las cantidades recibidas a las líneas de la orden de compra (avanza su estado de
   * recepción).
   */
  void applyReceipt(Long purchaseOrderId, Long companyId, List<ReceivedLine> lines, Long actorId);

  /** Revierte las cantidades recibidas de la orden al cancelar la recepción. */
  void revertReceipt(Long purchaseOrderId, Long companyId, List<ReceivedLine> lines, Long actorId);
}
