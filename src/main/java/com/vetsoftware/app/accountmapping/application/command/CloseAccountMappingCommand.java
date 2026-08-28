package com.vetsoftware.app.accountmapping.application.command;

import java.time.LocalDate;

/**
 * Poner fecha de fin a un mapeo vigente.
 *
 * <p>
 * <strong>Un mapeo no se edita: se cierra y se abre otro.</strong> Es la unica
 * forma de que las facturas ya asentadas conserven la explicacion de contra que
 * cuenta se asentaron, y es ademas lo que libera el hueco de
 * {@code uq_account_mappings_current} para publicar el relevo.
 */
public record CloseAccountMappingCommand(Long id, LocalDate validTo) {
}
