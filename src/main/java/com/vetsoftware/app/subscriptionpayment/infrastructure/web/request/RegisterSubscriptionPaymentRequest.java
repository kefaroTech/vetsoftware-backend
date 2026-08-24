package com.vetsoftware.app.subscriptionpayment.infrastructure.web.request;

import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <strong>Sin {@code companyId}, y no es un olvido</strong>: la empresa la pone
 * el controller desde el principal autenticado. Si viajara en el cuerpo, un
 * cliente podria registrar un pago a nombre de otra clinica.
 *
 * @param clientRequestId
 *            llave de idempotencia del operador. Con ella, el doble clic
 *            devuelve el pago que ya se creo en vez de cobrar dos veces
 */
public record RegisterSubscriptionPaymentRequest(
        @NotNull(message = "El valor del pago es obligatorio.") @Positive(message = "El valor del pago debe ser mayor que cero.") BigDecimal amount,
        @Size(min = 3, max = 3, message = "La moneda debe ser un codigo ISO de 3 letras.") String currency,
        @NotNull(message = "Debes indicar el medio de pago.") PaymentMethod paymentMethod,
        @Size(max = 40, message = "La pasarela no puede superar los 40 caracteres.") String gateway,
        @Size(max = 120, message = "La referencia de la pasarela no puede superar los 120 caracteres.") String gatewayReference,
        @NotNull(message = "Debes indicar cuando se recibio el pago.") LocalDateTime receivedAt,
        @Size(max = 64, message = "El identificador de la solicitud no puede superar los 64 caracteres.") String clientRequestId) {
}
