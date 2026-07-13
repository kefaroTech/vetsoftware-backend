package com.vetsoftware.app.goodsreceipt.application.port.out;

/** Cantidad recibida contra una línea de una orden de compra (para recepción parcial / progresiva). */
public record ReceivedLine(Long purchaseOrderLineId, int quantity) {}
