package com.vetsoftware.app.supplierwithholding.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * El acuse de la consignacion de lo retenido. Conservarlo es obligacion expresa
 * del art. 632 ET.
 */
public record RegisterSupplierWithholdingPaymentRequest(
        @NotBlank(message = "Debes indicar la referencia del recibo de pago.") @Size(max = 255, message = "La referencia del recibo no puede superar los 255 caracteres.") String paymentReceiptRef) {
}
