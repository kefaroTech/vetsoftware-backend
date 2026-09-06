package com.vetsoftware.app.paymentgateway.infrastructure.gateway;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code vetsoftware.payments.wompi.*}. Con {@code enabled = false} (el
 * default) el cliente falla cerrado con
 * {@code PaymentGatewayNotConfiguredException} en vez de intentar hablar con
 * una URL o unas credenciales vacías.
 */
@ConfigurationProperties("vetsoftware.payments.wompi")
public record WompiProperties(@DefaultValue("false") boolean enabled,
        @DefaultValue("https://sandbox.wompi.co/v1") String baseUrl,
        @DefaultValue("") String publicKey, @DefaultValue("") String privateKey,
        @DefaultValue("") String integritySecret, @DefaultValue("") String eventsSecret,
        @DefaultValue("6") int statusPollAttempts,
        @DefaultValue("2s") Duration statusPollInterval) {
}
