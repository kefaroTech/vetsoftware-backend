package com.vetsoftware.app.accountingaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * El cierre de una vigencia.
 *
 * @param validTo
 *            primer dia en que la cuenta <em>ya no</em> vale. Que sea posterior
 *            a {@code validFrom} lo comprueba el dominio: es una regla entre
 *            dos campos y uno de ellos no viaja en este cuerpo
 */
public record CloseAccountingAccountRequest(
        @NotNull(message = "Debes indicar desde cuando deja de aplicar la cuenta.") LocalDate validTo) {
}
