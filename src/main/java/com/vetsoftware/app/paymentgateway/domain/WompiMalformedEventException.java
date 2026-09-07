package com.vetsoftware.app.paymentgateway.domain;

/**
 * El cuerpo del webhook de Wompi no es JSON válido o no tiene la forma
 * esperada. Se mapea a 400, no 500: es un cuerpo mal formado, no un fallo del
 * servidor.
 */
public class WompiMalformedEventException extends RuntimeException {

    public WompiMalformedEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
