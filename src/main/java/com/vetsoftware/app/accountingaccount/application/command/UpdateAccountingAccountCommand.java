package com.vetsoftware.app.accountingaccount.application.command;

/**
 * Lo unico editable de una cuenta publicada.
 *
 * <p>
 * <strong>No lleva ni codigo, ni clase, ni nivel, ni padre</strong>, y esa
 * ausencia es la decision: los cuatro definen que significa la cuenta, y
 * cambiarlos reescribiria el sentido de todos los asientos que ya apuntan a
 * ella. Cuando el plan cambia se abre un codigo nuevo y se cierra el viejo.
 */
public record UpdateAccountingAccountCommand(Long id, String name, boolean requiresThirdParty) {
}
