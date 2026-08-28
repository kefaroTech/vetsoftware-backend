package com.vetsoftware.app.accountmapping.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * El cierre de una vigencia. Sin {@code id} —lo lleva la ruta—.
 *
 * @param validTo
 *            primer dia en que el mapeo <em>ya no</em> aplica. Que sea
 *            posterior a {@code validFrom} lo comprueba el dominio: es una
 *            regla entre dos campos y uno de ellos no viaja en este cuerpo
 */
public record CloseAccountMappingRequest(
        @NotNull(message = "Debes indicar desde cuando deja de aplicar el mapeo.") LocalDate validTo) {
}
