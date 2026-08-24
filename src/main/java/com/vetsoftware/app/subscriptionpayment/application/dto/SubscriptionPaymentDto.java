package com.vetsoftware.app.subscriptionpayment.application.dto;

import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubscriptionPaymentDto(Long id, Long companyId, BigDecimal amount, String currency,
        PaymentMethod paymentMethod, String gateway, String gatewayReference,
        LocalDateTime receivedAt, SubscriptionPaymentStatus status, LocalDateTime reconciledAt,
        LocalDateTime createdDate, Long version) {

    public static SubscriptionPaymentDto from(SubscriptionPayment payment) {
        return new SubscriptionPaymentDto(payment.getId(), payment.getCompanyId(),
                payment.getAmount(), payment.getCurrency(), payment.getPaymentMethod(),
                payment.getGateway(), payment.getGatewayReference(), payment.getReceivedAt(),
                payment.getStatus(), payment.getReconciledAt(), payment.getCreatedDate(),
                payment.getVersion());
    }
}
