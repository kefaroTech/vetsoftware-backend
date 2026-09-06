package com.vetsoftware.app.paymentgateway.domain;

/**
 * La pasarela está deshabilitada ({@code vetsoftware.payments.wompi.enabled:
 * false}) o sin credenciales. Se mapea a 409: el cuerpo de la petición está
 * bien formado, lo que falla es que no hay con qué cobrar.
 */
public class PaymentGatewayNotConfiguredException extends RuntimeException {

    public PaymentGatewayNotConfiguredException(String message) {
        super(message);
    }
}
