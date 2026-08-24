package com.vetsoftware.app.subscriptionpayment.infrastructure.web.request;

import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeSubscriptionPaymentStatusRequest(
        @NotNull(message = "Debes indicar el nuevo estado del pago.") SubscriptionPaymentStatus status) {
}
