package com.vetsoftware.app.paymentgateway.domain;

import java.time.Duration;

/**
 * Wompi respondió 429: se agotó el cupo de peticiones. Lleva el
 * {@code Retry-After} que la pasarela envió —o, si no lo envió, un mínimo de
 * una hora— para que el llamador reprograme el reintento en vez de insistir de
 * inmediato y empeorar el throttling.
 */
public class WompiRateLimitedException extends WompiGatewayException {

    private static final Duration MINIMUM_RETRY_AFTER = Duration.ofHours(1);

    private final Duration retryAfter;

    public WompiRateLimitedException(String message, Duration retryAfter) {
        super(message);
        this.retryAfter = retryAfter == null || retryAfter.compareTo(MINIMUM_RETRY_AFTER) < 0
                ? MINIMUM_RETRY_AFTER
                : retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
