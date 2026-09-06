package com.vetsoftware.app.paymentgateway.domain;

/**
 * El literal que va en la columna {@code gateway} de
 * {@code subscription_payment_methods}, {@code subscription_payments} y
 * {@code payment_attempts} para todo lo que pasa por esta pasarela.
 */
public final class PaymentGatewayNames {

    public static final String WOMPI = "WOMPI";

    private PaymentGatewayNames() {
    }
}
