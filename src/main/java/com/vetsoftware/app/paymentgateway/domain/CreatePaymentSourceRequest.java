package com.vetsoftware.app.paymentgateway.domain;

/** Cuerpo de {@code POST /payment_sources}. Siempre {@code type = "CARD"}. */
public record CreatePaymentSourceRequest(String cardToken, String customerEmail,
        String acceptanceToken, boolean acceptPersonalAuth) {
}
