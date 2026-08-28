package com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <strong>Sin fecha</strong>: la pone el servidor con el reloj inyectado. Es la
 * frontera a partir de la cual los cobros dejan de estar autorizados, y dejar
 * que la elija quien llama permitiria moverla hacia atras para desautorizar
 * cobros que si tenian mandato.
 *
 * <p>
 * El motivo se pide para poder explicar la revocacion despues, no como
 * condicion para aceptarla: revocar el debito automatico no exige justificarse.
 */
public record RevokeSubscriptionPaymentMethodRequest(
        @NotBlank(message = "Debes indicar el motivo de la revocacion.") @Size(max = 255, message = "El motivo no puede superar los 255 caracteres.") String reason) {
}
