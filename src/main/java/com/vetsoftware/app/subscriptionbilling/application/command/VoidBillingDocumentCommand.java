package com.vetsoftware.app.subscriptionbilling.application.command;

/**
 * Anular un documento <b>antes</b> de que exista fuera. Uno con factura externa
 * ya registrada no se anula: se corrige con una nota crédito encadenada.
 */
public record VoidBillingDocumentCommand(Long id, Long companyId) {
}
