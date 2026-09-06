package com.vetsoftware.app.paymentgateway.application.port.out;

import com.vetsoftware.app.paymentgateway.domain.ParsedWompiEvent;

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
}
