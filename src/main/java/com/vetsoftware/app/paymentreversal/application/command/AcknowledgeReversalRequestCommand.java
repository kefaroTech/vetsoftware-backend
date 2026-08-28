package com.vetsoftware.app.paymentreversal.application.command;

/**
 * @param acknowledgementRef
 *            la constancia que se le entrega al cliente. La fecha la pone el
 *            servidor con el reloj inyectado, no el cliente
 */
public record AcknowledgeReversalRequestCommand(Long id, Long companyId,
        String acknowledgementRef) {
}
