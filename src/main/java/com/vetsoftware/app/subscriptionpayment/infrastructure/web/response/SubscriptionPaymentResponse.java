package com.vetsoftware.app.subscriptionpayment.infrastructure.web.response;

import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubscriptionPaymentResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal amount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String currency,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PaymentMethod paymentMethod,
        String gateway, String gatewayReference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime receivedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SubscriptionPaymentStatus status,
        LocalDateTime reconciledAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        Long version) {

    public static SubscriptionPaymentResponse from(SubscriptionPaymentDto dto) {
        return new SubscriptionPaymentResponse(dto.id(), dto.companyId(), dto.amount(),
                dto.currency(), dto.paymentMethod(), dto.gateway(), dto.gatewayReference(),
                dto.receivedAt(), dto.status(), dto.reconciledAt(), dto.createdDate(),
                dto.version());
    }
}
