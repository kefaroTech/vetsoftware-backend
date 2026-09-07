package com.vetsoftware.app.subscriptionpayment.application.command;

/**
 * @param reason
 *            por que se revierte. Obligatorio: un reverso sin motivo deja al
 *            auditor reconstruyendolo a mano cruzando el resto del expediente
 *            en vez de leerlo del evento AUDIT que se supone lo documenta.
 */
public record ReverseBillingDocumentApplicationCommand(Long applicationId, Long companyId,
        String reason) {
}
