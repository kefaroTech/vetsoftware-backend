package com.vetsoftware.app.paymentgateway.application.dto;

import com.vetsoftware.app.paymentgateway.domain.FirstPeriodPaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Lo que el tenant ve del cobro del primer periodo, para el paso de éxito. */
public record FirstPeriodPaymentDto(FirstPeriodPaymentStatus status, BigDecimal amount,
        String currency, String gatewayReference, LocalDateTime attemptedAt) {
}
