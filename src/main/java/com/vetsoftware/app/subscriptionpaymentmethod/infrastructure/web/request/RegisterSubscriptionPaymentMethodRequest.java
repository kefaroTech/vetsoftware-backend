package com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.web.request;

import com.vetsoftware.app.subscriptionpaymentmethod.domain.PaymentMethodKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <strong>Sin {@code companyId}, y no es un olvido</strong>: la empresa la pone
 * el controller desde el principal autenticado. Si viajara en el cuerpo, un
 * cliente podria registrar una tarjeta a nombre de otra clinica —y con ella
 * cobrarle—.
 *
 * @param token
 *            el testigo que devuelve la pasarela. <strong>Nunca el numero de la
 *            tarjeta</strong>: el PAN no entra en este backend por ningun
 *            campo, y la tokenizacion ocurre antes, contra la pasarela
 * @param mandateEvidence
 *            constancia de la autorizacion expresa. Obligatoria: la ley exige
 *            autorizacion expresa —el silencio no vale— y sin constancia la
 *            autorizacion es una afirmacion propia
 */
public record RegisterSubscriptionPaymentMethodRequest(
        @NotNull(message = "Debes indicar el tipo de medio de pago.") PaymentMethodKind methodKind,
        @NotBlank(message = "La pasarela es obligatoria.") @Size(max = 40, message = "La pasarela no puede superar los 40 caracteres.") String gateway,
        @NotBlank(message = "El token de la pasarela es obligatorio.") @Size(max = 255, message = "El token no puede superar los 255 caracteres.") String token,
        @Size(max = 30, message = "La franquicia no puede superar los 30 caracteres.") String brand,
        @Pattern(regexp = "^[0-9]{4}$", message = "Los ultimos cuatro digitos deben ser exactamente cuatro numeros.") String lastFour,
        LocalDate expiresOn,
        @NotBlank(message = "La constancia de la autorizacion es obligatoria.") @Size(max = 255, message = "La constancia no puede superar los 255 caracteres.") String mandateEvidence,
        @NotNull(message = "Debes indicar cuando se autorizo el cobro.") LocalDateTime authorizedAt) {
}
