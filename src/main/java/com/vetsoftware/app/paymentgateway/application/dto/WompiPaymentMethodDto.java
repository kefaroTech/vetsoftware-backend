package com.vetsoftware.app.paymentgateway.application.dto;

import java.time.LocalDate;

/**
 * Proyección de salida del medio de pago recién dado de alta.
 *
 * <p>
 * <strong>Sin el testigo de la pasarela</strong>, mismo criterio que
 * {@code SubscriptionPaymentMethodDto}.
 */
public record WompiPaymentMethodDto(Long paymentMethodId, String brand, String lastFour,
        LocalDate expiresOn, boolean defaultMethod) {
}
