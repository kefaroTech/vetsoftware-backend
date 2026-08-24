package com.vetsoftware.app.subscription.infrastructure.web.response;

import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import java.time.LocalDateTime;

/** Una linea de la pelicula del contrato. */
public record SubscriptionStatusChangeResponse(Long id, Long companyId, Long subscriptionId,
        SubscriptionStatus fromStatus, SubscriptionStatus toStatus, String reason,
        LocalDateTime occurredAt, String actor, LocalDateTime createdDate) {
}
