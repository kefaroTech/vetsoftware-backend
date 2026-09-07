package com.vetsoftware.app.paymentgateway.domain;

/**
 * El cuerpo de un webhook de Wompi supera
 * {@code vetsoftware.payments.wompi.max-event-body-bytes}, detectado mientras
 * se lee porque la petición no declaró {@code Content-Length}. Se mapea a 413,
 * no 500: es un límite de tamaño, no un fallo del servidor.
 */
public class WompiWebhookBodyTooLargeException extends RuntimeException {

    public WompiWebhookBodyTooLargeException(String message) {
        super(message);
    }
}
