package com.vetsoftware.app.paymentgateway.application.port.out;

import com.vetsoftware.app.paymentgateway.domain.ParsedWompiEvent;
import java.time.Duration;

/**
 * Interpreta y autentica un webhook de Wompi. Separado de
 * {@link PaymentGatewayPort} porque este no habla con Wompi por red: solo lee
 * el cuerpo que Wompi ya entregó y lo contrasta con el secreto de eventos.
 */
public interface WompiEventPort {

    ParsedWompiEvent parse(String rawBody);

    /**
     * {@code true} si {@code checksumHeader} coincide con el checksum del evento.
     */
    boolean matchesChecksum(ParsedWompiEvent event, String checksumHeader);

    /**
     * El checksum que Wompi habría calculado para este evento con el secreto real:
     * la llave de idempotencia de {@code gateway_webhook_events}, sea o no el que
     * trajo {@code X-Event-Checksum}.
     */
    String computeChecksum(ParsedWompiEvent event);

    /**
     * Falla cerrado con {@code PaymentGatewayNotConfiguredException} si Wompi está
     * deshabilitado o el secreto de eventos está en blanco: calcular el checksum
     * contra un secreto vacío lo haría forjable por cualquiera.
     */
    void requireConfigured();

    Duration freshnessTolerance();
}
