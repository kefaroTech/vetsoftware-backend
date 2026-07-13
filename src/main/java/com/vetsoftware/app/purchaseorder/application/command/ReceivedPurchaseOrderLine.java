package com.vetsoftware.app.purchaseorder.application.command;

/** Una línea recibida dentro de {@link ApplyReceiptCommand}: cuánto llegó de una línea concreta de la orden. */
public record ReceivedPurchaseOrderLine(Long purchaseOrderLineId, int quantity) {}
