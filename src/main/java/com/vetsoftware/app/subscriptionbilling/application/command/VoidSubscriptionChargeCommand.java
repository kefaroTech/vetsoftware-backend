package com.vetsoftware.app.subscriptionbilling.application.command;

/**
 * Anular un cargo <b>creando el que lo compensa</b>, nunca editándolo ni
 * borrándolo. Los dos quedan y suman cero.
 */
public record VoidSubscriptionChargeCommand(Long id, Long companyId, String description) {
}
