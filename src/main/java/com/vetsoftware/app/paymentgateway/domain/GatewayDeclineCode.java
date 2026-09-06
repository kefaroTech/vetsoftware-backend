package com.vetsoftware.app.paymentgateway.domain;

/**
 * El código que se le anota a {@code PaymentAttempt} para un rechazo de Wompi.
 *
 * <p>
 * <strong>Wompi no siempre manda {@code status_message}.</strong> Con un
 * {@code DECLINED}/{@code ERROR}/{@code VOIDED} sin motivo, se anota el propio
 * estado como código: {@code chk_payment_attempts_declined_by_gateway} exige
 * que todo rechazo imputable al cliente lleve un código no vacío, y un intento
 * sin ninguna pista es peor que uno con el estado crudo de la pasarela.
 */
public final class GatewayDeclineCode {

    private GatewayDeclineCode() {
    }

    public static String resolve(String statusMessage, GatewayTransactionStatus status) {
        return (statusMessage == null || statusMessage.isBlank()) ? status.name() : statusMessage;
    }
}
