package com.vetsoftware.app.paymentgateway.domain;

/**
 * Envuelve cualquier fallo de red o HTTP al hablar con Wompi. Existe para que
 * {@code RestClientException} nunca se propague fuera de
 * {@code infrastructure}: quien llama al puerto no debería saber que el
 * transporte es HTTP.
 */
public class WompiGatewayException extends RuntimeException {

    public WompiGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
