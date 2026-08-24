package com.vetsoftware.app.quote.application.command;

/**
 * Paso SENT -> ACCEPTED con la prueba de la aceptacion.
 *
 * @param acceptedIp
 *            IP desde la que se acepto. La pone el controller desde la peticion
 *            HTTP, no el cuerpo: un campo de formulario con la IP no prueba
 *            nada.
 */
public record AcceptQuoteCommand(Long id, Long companyId, String acceptedByEmail,
        String acceptedIp) {
}
