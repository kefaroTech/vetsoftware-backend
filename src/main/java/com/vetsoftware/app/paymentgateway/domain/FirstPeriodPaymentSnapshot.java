package com.vetsoftware.app.paymentgateway.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * El pago de otra feature ({@code subscriptionpayment}) visto desde aquí para
 * la idempotencia del primer cobro y para
 * {@code FindFirstPeriodPaymentUseCase}.
 *
 * @param status
 *            el nombre crudo de {@code SubscriptionPaymentStatus}
 *            ({@code PENDING|CONFIRMED|FAILED|REFUNDED}). Viaja como texto y no
 *            como el enum de esa feature: este slice no importa su dominio,
 *            solo su repositorio JPA.
 */
public record FirstPeriodPaymentSnapshot(Long paymentId, Long companyId, String status,
        BigDecimal amount, String currency, String gatewayReference, LocalDateTime receivedAt) {
}
