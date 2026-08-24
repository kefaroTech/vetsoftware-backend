package com.vetsoftware.app.subscription.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cancelacion, con sus <strong>dos</strong> fechas.
 *
 * <p>
 * {@code requestedAt} es cuando lo pidio y {@code effectiveDate} cuando se va.
 * Van separadas porque el cliente cancela el 10 y se queda hasta el 30, que es
 * lo que ya pago. {@code reason} es informacion de negocio, no burocracia: es
 * la unica fuente que dice por que se van los clientes.
 *
 * <p>
 * Quien firma la baja no viaja en el cuerpo: lo inyecta el controller desde el
 * principal. El numero del otrosi tampoco: lo reserva el servidor. Y los
 * importes tampoco: el abono por los dias que quedaban sin devengar lo calcula
 * {@code ProrationCalculator} sobre las lineas vigentes del contrato.
 */
public record CancelSubscriptionRequest(@NotNull LocalDateTime requestedAt,
        @NotNull LocalDate effectiveDate, @Size(max = 255) String reason,
        @NotBlank @Size(max = 64) String clientRequestId) {
}
