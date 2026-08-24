package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * Se intento emitir un documento de cobro sin ningun cargo detras. HTTP 409.
 *
 * <p>
 * Un cobro que no agrupa ningun devengo es un cobro que nadie puede explicar, y
 * es lo que caza el {@code COUNT(c.id) = 0} de la vigilancia R6. Fallar al
 * emitirlo es mas barato que descubrirlo en la conciliacion del mes siguiente.
 */
public class EmptyBillingDocumentException extends RuntimeException {
    public EmptyBillingDocumentException(Long subscriptionId) {
        super("No pending charges to bill for subscription " + subscriptionId);
    }
}
