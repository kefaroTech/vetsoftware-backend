package com.vetsoftware.app.supplierwithholding.application.command;

/**
 * Anotar la prueba de la consignacion de lo retenido.
 *
 * <p>
 * Es un documento que <b>llega tarde</b>, y por eso la fila se reescribe.
 * Conservar el recibo de pago es obligacion expresa del art. 632 ET: sin el no
 * se puede probar que lo retenido se consigno.
 */
public record RegisterSupplierWithholdingPaymentCommand(Long id, String paymentReceiptRef) {
}
