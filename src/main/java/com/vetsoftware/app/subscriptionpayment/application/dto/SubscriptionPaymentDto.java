package com.vetsoftware.app.subscriptionpayment.application.dto;

import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SubscriptionPaymentDto(Long id, Long companyId, BigDecimal amount, String currency,
        PaymentMethod paymentMethod, String gateway, String gatewayReference,
        LocalDateTime receivedAt, SubscriptionPaymentStatus status, LocalDateTime reconciledAt,
        BigDecimal feeAmount, BigDecimal netAmount, String settlementReference, LocalDate settledOn,
        BigDecimal refundedAmount, String clientRequestId, boolean reservation,
        LocalDateTime createdDate, Long version) {

    public static SubscriptionPaymentDto from(SubscriptionPayment payment) {
        boolean reservation = payment.getStatus() == SubscriptionPaymentStatus.PENDING
                && payment.getGateway() != null && payment.getGatewayReference() == null;
        return new SubscriptionPaymentDto(payment.getId(), payment.getCompanyId(),
                payment.getAmount(), payment.getCurrency(), payment.getPaymentMethod(),
                payment.getGateway(), payment.getGatewayReference(), payment.getReceivedAt(),
                payment.getStatus(), payment.getReconciledAt(), payment.getFeeAmount(),
                payment.getNetAmount(), payment.getSettlementReference(), payment.getSettledOn(),
                payment.getRefundedAmount(), payment.getClientRequestId(), reservation,
                payment.getCreatedDate(), payment.getVersion());
    }
}
