package com.vetsoftware.app.paymentgateway.infrastructure.gateway;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code vetsoftware.payments.wompi.*}. Con {@code enabled = false} (el
 * default) el cliente falla cerrado con
 * {@code PaymentGatewayNotConfiguredException} en vez de intentar hablar con
 * una URL o unas credenciales vacías.
 *
 * <p>
 * Con {@code enabled = true} una credencial vacía tumba el arranque: para Wompi
 * no es «sin configurar» sino un 401 en cada checkout que nadie ve hasta horas
 * después en el tenant. Fallar aquí hace que el despliegue falle y haga
 * rollback.
 */
@ConfigurationProperties("vetsoftware.payments.wompi")
public record WompiProperties(boolean enabled, String baseUrl, String publicKey, String privateKey,
        String integritySecret, String eventsSecret, int statusPollAttempts,
        Duration statusPollInterval, Duration eventFreshnessTolerance, long maxEventBodyBytes,
        Duration pendingTransactionMaxAge) {

    @ConstructorBinding
    public WompiProperties(@DefaultValue("false") boolean enabled,
            @DefaultValue("https://sandbox.wompi.co/v1") String baseUrl,
            @DefaultValue("") String publicKey, @DefaultValue("") String privateKey,
            @DefaultValue("") String integritySecret, @DefaultValue("") String eventsSecret,
            @DefaultValue("6") int statusPollAttempts,
            @DefaultValue("2s") Duration statusPollInterval,
            @DefaultValue("24h") Duration eventFreshnessTolerance,
            @DefaultValue("65536") long maxEventBodyBytes,
            @DefaultValue("24h") Duration pendingTransactionMaxAge) {
        if (enabled) {
            requireConfigured(publicKey, "public-key");
            requireConfigured(privateKey, "private-key");
            requireConfigured(integritySecret, "integrity-secret");
            requireConfigured(eventsSecret, "events-secret");
        }
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.integritySecret = integritySecret;
        this.eventsSecret = eventsSecret;
        this.statusPollAttempts = statusPollAttempts;
        this.statusPollInterval = statusPollInterval;
        this.eventFreshnessTolerance = eventFreshnessTolerance;
        this.maxEventBodyBytes = maxEventBodyBytes;
        this.pendingTransactionMaxAge = pendingTransactionMaxAge;
    }

    private static void requireConfigured(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Wompi habilitado sin vetsoftware.payments.wompi."
                    + property + ": la aplicación no arranca para no llamar a la pasarela con"
                    + " credenciales vacías");
        }
    }
}
