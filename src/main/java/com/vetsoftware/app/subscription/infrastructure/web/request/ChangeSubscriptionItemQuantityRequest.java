package com.vetsoftware.app.subscription.infrastructure.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Cambio de cantidad. No se manda precio: el de la linea original viaja intacto
 * a la sucesora, porque lo que se renegocio fue cuantas unidades.
 *
 * <p>
 * Quien firma el otrosi no viaja en el cuerpo: lo inyecta el controller desde
 * el principal. El numero del otrosi tampoco: lo reserva el servidor. Y los
 * importes tampoco: el servidor los deriva de la diferencia entre la linea
 * sucesora y la original, prorrateada por los dias que quedan del periodo.
 */
public record ChangeSubscriptionItemQuantityRequest(@NotNull Long subscriptionItemId,
        @NotNull @Min(1) Integer newQuantity, @NotBlank @Size(max = 64) String clientRequestId,
        @NotNull LocalDate effectiveDate, @Size(max = 255) String reason) {
}
