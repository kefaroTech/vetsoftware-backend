package com.vetsoftware.app.quote.domain;

/**
 * Transicion de estado no permitida: enviar una cotizacion ya aceptada, aceptar
 * una que nunca se envio, borrar una que ya salio al cliente.
 *
 * <p>
 * Extiende {@link IllegalStateException} a proposito: mientras
 * GlobalExceptionHandler no le de su propio handler, el desague generico ya la
 * traduce a 409 INVALID_STATE en vez de dejarla caer a un 500. Un handler
 * dedicado con codigo propio la seguira capturando primero, porque el mas
 * especifico gana.
 */
public class InvalidQuoteStatusTransitionException extends IllegalStateException {
    public InvalidQuoteStatusTransitionException(QuoteStatus from, QuoteStatus to) {
        super("Invalid quote status transition: " + from + " -> " + to);
    }
}
