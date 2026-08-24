package com.vetsoftware.app.subscription.application.dto;

import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChange;
import java.time.LocalDateTime;

/** Una linea de la pelicula del contrato. */
public record SubscriptionStatusChangeDto(Long id, Long companyId, Long subscriptionId,
        SubscriptionStatus fromStatus, SubscriptionStatus toStatus, String reason,
        LocalDateTime occurredAt, String actor, LocalDateTime createdDate) {

    public static SubscriptionStatusChangeDto from(SubscriptionStatusChange change) {
        return new SubscriptionStatusChangeDto(change.getId(), change.getCompanyId(),
                change.getSubscriptionId(), change.getFromStatus(), change.getToStatus(),
                change.getReason(), change.getOccurredAt(), change.getActor(),
                change.getCreatedDate());
    }
}
