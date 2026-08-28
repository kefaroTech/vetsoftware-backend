package com.vetsoftware.app.accountingaccount.application.command;

import java.time.LocalDate;

/**
 * Poner fecha de fin a la vigencia de una cuenta.
 *
 * <p>
 * <strong>Cerrar no es borrar, y esa es toda la diferencia.</strong> La cuenta
 * que dejo de usarse el 1 de enero sigue siendo la correcta para los asientos
 * de diciembre; borrarla o deshabilitarla los dejaria sin explicacion. Ademas
 * {@code fk_account_mappings_debit}, {@code _credit} y {@code _deferred} son
 * {@code RESTRICT}: un borrado ni siquiera seria posible mientras algun mapeo
 * la referencie.
 *
 * @param validTo
 *            primer dia en que la cuenta <em>ya no</em> vale. Estrictamente
 *            posterior a {@code validFrom}
 */
public record CloseAccountingAccountCommand(Long id, LocalDate validTo) {
}
