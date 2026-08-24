package com.vetsoftware.app.subscription.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Baja de linea. {@code effectiveDate} es la fecha de fin que se escribe en la
 * fila: la linea no se borra, se cierra.
 *
 * <p>
 * Quien firma el otrosi no viaja en el cuerpo: lo inyecta el controller desde
 * el principal. El numero del otrosi tampoco: lo reserva el servidor. Y los
 * importes tampoco: el abono por los dias que la linea deja de servir lo
 * calcula el servidor con el precio congelado en la propia fila.
 */
public record RemoveSubscriptionItemRequest(@NotNull Long subscriptionItemId,
        @NotBlank @Size(max = 64) String clientRequestId, @NotNull LocalDate effectiveDate,
        @Size(max = 255) String reason) {
}
