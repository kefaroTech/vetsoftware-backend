package com.vetsoftware.app.subscriptionpayment.application.command;

import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;

public record ChangeSubscriptionPaymentStatusCommand(Long id, Long companyId,
        SubscriptionPaymentStatus status) {
}
