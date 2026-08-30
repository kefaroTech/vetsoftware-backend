package com.vetsoftware.app.subscription.application.command;

/**
 * Sustituye el contrato vigente de una empresa por el que acaba de firmar al
 * aceptar una cotizacion (DC-2).
 *
 * <p>
 * <strong>No trae ni un solo termino del contrato</strong> —ni tarifa, ni
 * ciclo, ni estado, ni fechas, ni lineas— y eso es deliberado: todo eso lo
 * dicta la oferta que el cliente acepto, y esta rodaja lo lee del snapshot. Un
 * command con terminos seria una via para firmar algo distinto de lo que se
 * acepto, que es exactamente lo que
 * {@link com.vetsoftware.app.subscription.application.port.in.CreateRequestedSubscriptionUseCase}
 * mantiene cerrado a plataforma.
 *
 * @param quoteId
 *            la oferta aceptada. Es la unica fuente de los terminos
 * @param companyId
 *            la empresa que firma. Acota la carga del snapshot, asi que una
 *            cotizacion de otra empresa no se encuentra en vez de encontrarse y
 *            rechazarse
 */
public record ReplaceSubscriptionFromQuoteCommand(Long quoteId, Long companyId) {
}
