package com.vetsoftware.app.paymentgateway.domain;

/**
 * Cuerpo de {@code POST /payment_sources}. Siempre {@code type = "CARD"}.
 *
 * <p>
 * {@code personalDataAuthToken} viaja como {@code accept_personal_auth}: pese
 * al nombre, Wompi no espera un booleano sino el token firmado de
 * {@code presigned_personal_data_auth}, y responde 422 con cualquier otro tipo.
 */
public record CreatePaymentSourceRequest(String cardToken, String customerEmail,
        String acceptanceToken, String personalDataAuthToken) {
}
