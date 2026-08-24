package com.vetsoftware.app.dunning.infrastructure.persistence;

import com.vetsoftware.app.dunning.domain.BillingDocumentRef;
import com.vetsoftware.app.dunning.domain.DunningEvent;
import com.vetsoftware.app.dunning.domain.SubscriptionRef;
import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionJpaEntity;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class DunningEventJpaMapper {

    public DunningEventJpaEntity toJpa(DunningEvent event, SubscriptionJpaEntity subscription,
            SubscriptionBillingDocumentJpaEntity billingDocument) {
        DunningEventJpaEntity entity = new DunningEventJpaEntity();
        entity.setId(event.getId());
        entity.setCompanyId(event.getCompanyId());
        entity.setSubscription(subscription);
        entity.setBillingDocument(billingDocument);
        entity.setEventType(event.getEventType());
        entity.setDaysOverdue(event.getDaysOverdue());
        entity.setChannel(event.getChannel());
        entity.setDetail(event.getDetail());
        entity.setOccurredAt(event.getOccurredAt());
        entity.setCreatedDate(event.getCreatedDate());
        return entity;
    }

    /** Camino de lectura: el {@code @EntityGraph} ya hidrato las asociaciones. */
    public DunningEvent toDomain(DunningEventJpaEntity entity) {
        return toDomain(entity, DunningRefs.toRef(entity.getSubscription()),
                DunningRefs.toRef(entity.getBillingDocument()));
    }

    /**
     * Camino de escritura: reusa los {@code Ref} ya resueltos por el caso de uso
     * para no hidratar los proxies de {@code getReferenceById}.
     */
    public DunningEvent toDomain(DunningEventJpaEntity entity, SubscriptionRef subscription,
            BillingDocumentRef billingDocument) {
        return new DunningEvent(entity.getId(), entity.getCompanyId(), subscription,
                billingDocument, entity.getEventType(), entity.getDaysOverdue(),
                entity.getChannel(), entity.getDetail(), entity.getOccurredAt(),
                entity.getCreatedDate());
    }
}
