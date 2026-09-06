package com.vetsoftware.app.paymentgateway.infrastructure.web.response;

import com.vetsoftware.app.paymentgateway.application.dto.FirstPeriodPaymentDto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FirstPeriodPaymentResponse(String status, BigDecimal amount, String currency,
        String gatewayReference, LocalDateTime attemptedAt) {

    public static FirstPeriodPaymentResponse from(FirstPeriodPaymentDto dto) {
        return new FirstPeriodPaymentResponse(dto.status().name(), dto.amount(), dto.currency(),
                dto.gatewayReference(), dto.attemptedAt());
    }
}
