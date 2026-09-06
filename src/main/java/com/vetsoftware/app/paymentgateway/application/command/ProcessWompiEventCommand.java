package com.vetsoftware.app.paymentgateway.application.command;

/**
 * Un webhook de Wompi tal como llegó.
 *
 * @param rawBody
 *            el cuerpo <strong>crudo</strong>, tal cual lo mandó Wompi: el
 *            checksum se calcula sobre los valores JSON, no sobre una
 *            representación reserializada
 * @param checksumHeader
 *            el valor de {@code X-Event-Checksum}
 */
public record ProcessWompiEventCommand(String rawBody, String checksumHeader) {
}
