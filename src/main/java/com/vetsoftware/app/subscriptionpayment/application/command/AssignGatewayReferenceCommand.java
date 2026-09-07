package com.vetsoftware.app.subscriptionpayment.application.command;

import java.time.LocalDateTime;

/** Ver {@code SubscriptionPayment#assignGatewayReference}. */
public record AssignGatewayReferenceCommand(Long paymentId, Long companyId, String gatewayReference,
        LocalDateTime gatewayCreatedAt) {
}
