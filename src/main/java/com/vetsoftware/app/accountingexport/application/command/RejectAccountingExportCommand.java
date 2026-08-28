package com.vetsoftware.app.accountingexport.application.command;

/**
 * El contador devolvio el fichero.
 *
 * <p>
 * <strong>El motivo es obligatorio</strong>, espejo de la tercera rama de
 * {@code chk_accounting_exports_lifecycle}: un rechazo sin motivo escrito
 * obliga a rehacer el fichero a ciegas. Rechazar es ademas lo que libera el
 * hueco de {@code uq_accounting_exports_current} para el siguiente intento.
 */
public record RejectAccountingExportCommand(Long id, String rejectionReason) {
}
