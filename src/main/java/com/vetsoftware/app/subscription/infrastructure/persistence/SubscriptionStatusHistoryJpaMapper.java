package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChange;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/** Unico sitio que conoce a la vez la bitacora de dominio y su entidad JPA. */
@Component
public class SubscriptionStatusHistoryJpaMapper {

    private final Clock clock;

    public SubscriptionStatusHistoryJpaMapper(Clock clock) {
        this.clock = clock;
    }

    public SubscriptionStatusHistoryJpaEntity toJpa(SubscriptionStatusChange change,
            CompanyJpaEntity company, SubscriptionJpaEntity subscription) {
        SubscriptionStatusHistoryJpaEntity entity = new SubscriptionStatusHistoryJpaEntity();
        entity.setId(change.getId());
        entity.setCompany(company);
        entity.setSubscription(subscription);
        entity.setFromStatus(change.getFromStatus());
        entity.setToStatus(change.getToStatus());
        entity.setReason(change.getReason());
        entity.setOccurredAt(change.getOccurredAt());
        entity.setActor(change.getActor());
        entity.setCreatedDate(change.getCreatedDate() == null
                ? LocalDateTime.now(clock)
                : change.getCreatedDate());
        return entity;
    }

    public SubscriptionStatusChange toDomain(SubscriptionStatusHistoryJpaEntity entity) {
        return new SubscriptionStatusChange(entity.getId(), entity.getCompany().getId(),
                entity.getSubscription().getId(), entity.getFromStatus(), entity.getToStatus(),
                entity.getReason(), entity.getOccurredAt(), entity.getActor(),
                entity.getCreatedDate());
    }
}
