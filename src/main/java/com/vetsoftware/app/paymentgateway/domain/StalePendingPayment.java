package com.vetsoftware.app.paymentgateway.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code gatewayReference} es {@code null} si el {@code POST /transactions}
 * nunca respondió.
 */
public record StalePendingPayment(Long companyId, Long paymentId, String gatewayReference,
        BigDecimal amount, String clientRequestId, LocalDateTime receivedAt) {
}
