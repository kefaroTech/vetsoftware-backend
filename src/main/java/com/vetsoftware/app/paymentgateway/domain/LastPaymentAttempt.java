package com.vetsoftware.app.paymentgateway.domain;

import java.time.LocalDateTime;

/**
 * El último intento de cobro de un documento, que es de otra feature
 * ({@code paymentattempt}), visto desde aquí.
 */
public record LastPaymentAttempt(int attemptNumber, GatewayDeclineKind declineKind,
        LocalDateTime attemptedAt, LocalDateTime nextAttemptAt) {
}
