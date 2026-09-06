package com.vetsoftware.app.paymentgateway.infrastructure.web.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta de la tarjeta ya tokenizada en el navegador. {@code companyId} no viaja
 * aquí: lo pone el controller con {@code authz.currentCompanyId()}.
 */
public record WompiPaymentSourceRequest(@NotBlank @Size(max = 120) String cardToken,
        @NotBlank String acceptanceToken, @NotBlank String personalDataAuthToken,
        @NotBlank @Size(max = 30) String brand, @Pattern(regexp = "^[0-9]{4}$") String lastFour,
        @Min(1) @Max(12) int expMonth, @Min(2026) @Max(2099) int expYear) {
}
