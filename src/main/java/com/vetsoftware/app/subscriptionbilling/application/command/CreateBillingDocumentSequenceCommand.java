package com.vetsoftware.app.subscriptionbilling.application.command;

/**
 * Declarar una serie del consecutivo interno.
 *
 * <p>
 * <b>Sin {@code companyId}</b>: el consecutivo es un contador global de
 * plataforma. Es también el motivo de que este caso de uso esté cerrado a
 * {@code hasRole("SYSTEM")} a secas.
 */
public record CreateBillingDocumentSequenceCommand(String prefix) {
}
