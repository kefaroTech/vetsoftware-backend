package com.vetsoftware.app.paymentgateway.application.dto;

/**
 * Lo que el navegador necesita para tokenizar la tarjeta directo contra Wompi:
 * la llave <strong>pública</strong> (nunca la privada) y los dos tokens de
 * aceptación vigentes.
 */
public record WompiCheckoutConfigDto(String environment, String apiBaseUrl, String publicKey,
        Acceptance acceptance, Acceptance personalDataAuthorization) {

    public record Acceptance(String token, String permalink) {
    }
}
