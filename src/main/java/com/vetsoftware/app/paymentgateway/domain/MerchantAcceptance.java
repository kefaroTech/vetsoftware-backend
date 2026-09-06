package com.vetsoftware.app.paymentgateway.domain;

/**
 * Los dos tokens de aceptación que devuelve {@code GET /merchants/{public_key}}
 * ({@code presigned_acceptance} y {@code presigned_personal_data_auth}). Son
 * JWT de vigencia corta: se piden en cada alta, nunca se cachean.
 */
public record MerchantAcceptance(String acceptanceToken, String acceptancePermalink,
        String personalDataAuthToken, String personalDataAuthPermalink) {
}
