package com.vetsoftware.app.purchaseorder.application.command;

import java.util.List;

/**
 * Comando INTERNO que la feature {@code goodsreceipt} usa para aplicar (o revertir) una recepción de mercancía
 * sobre una orden de compra, dentro de su propia transacción ya autorizada.
 */
public record ApplyReceiptCommand(
        Long purchaseOrderId,
        Long companyId,
        List<ReceivedPurchaseOrderLine> lines,
        Long actorId
) {}
