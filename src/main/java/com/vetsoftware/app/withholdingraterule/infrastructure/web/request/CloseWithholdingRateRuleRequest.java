package com.vetsoftware.app.withholdingraterule.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * El cierre de una vigencia. Sin {@code id} —lo lleva la ruta— y sin
 * {@code companyId}, que en este catalogo global no existe.
 *
 * @param validTo
 *            primer dia en que la tarifa <em>ya no</em> aplica. Que sea
 *            posterior a {@code validFrom} lo comprueba el dominio: es una
 *            regla entre dos campos y uno de ellos no viaja en este cuerpo
 */
public record CloseWithholdingRateRuleRequest(
        @NotNull(message = "Debes indicar desde cuando deja de aplicar la tarifa.") LocalDate validTo) {
}
