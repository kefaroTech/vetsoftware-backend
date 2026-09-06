package com.vetsoftware.app.paymentgateway.domain;

/**
 * Cuerpo de {@code POST /transactions}. Siempre {@code recurrent = true}
 * (fuente de pago tokenizada, no primer cobro con tarjeta suelta).
 *
 * <p>
 * <strong>Sin {@code signature}.</strong> La firma de integridad exige el
 * secreto de integridad, que es una credencial de infraestructura: la calcula
 * el adaptador ({@code WompiGatewayClient}), nunca el caso de uso.
 */
public record ChargeRequest(Long paymentSourceId, long amountInCents, String currency,
        String reference, String customerEmail) {
}
