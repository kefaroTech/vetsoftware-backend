package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.subscription.domain.SubscriptionAmendment;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/** Unico sitio que conoce a la vez el otrosi de dominio y su entidad JPA. */
@Component
public class SubscriptionAmendmentJpaMapper {

    private final Clock clock;

    public SubscriptionAmendmentJpaMapper(Clock clock) {
        this.clock = clock;
    }

    public SubscriptionAmendmentJpaEntity toJpa(SubscriptionAmendment amendment,
            CompanyJpaEntity company, SubscriptionJpaEntity subscription) {
        SubscriptionAmendmentJpaEntity entity = new SubscriptionAmendmentJpaEntity();
        entity.setId(amendment.getId());
        entity.setCompany(company);
        entity.setSubscription(subscription);
        entity.setAmendmentNumber(amendment.getAmendmentNumber());
        entity.setAmendmentType(amendment.getAmendmentType());
        entity.setEffectiveDate(amendment.getEffectiveDate());
        entity.setReason(amendment.getReason());
        entity.setRequestedByEmployeeId(amendment.getRequestedByEmployeeId());
        entity.setRequestedBySystemUserId(amendment.getRequestedBySystemUserId());
        entity.setProrationAmount(amendment.getProrationAmount());
        entity.setMonthlyDeltaAmount(amendment.getMonthlyDeltaAmount());
        entity.setQuoteId(amendment.getQuoteId());
        entity.setClientRequestId(amendment.getClientRequestId());
        entity.setCreatedDate(amendment.getCreatedDate() == null
                ? LocalDateTime.now(clock)
                : amendment.getCreatedDate());
        return entity;
    }

    public SubscriptionAmendment toDomain(SubscriptionAmendmentJpaEntity entity) {
        return new SubscriptionAmendment(entity.getId(), entity.getCompany().getId(),
                entity.getSubscription().getId(), entity.getAmendmentNumber(),
                entity.getAmendmentType(), entity.getEffectiveDate(), entity.getReason(),
                entity.getRequestedByEmployeeId(), entity.getRequestedBySystemUserId(),
                entity.getProrationAmount(), entity.getMonthlyDeltaAmount(), entity.getQuoteId(),
                entity.getClientRequestId(), entity.getCreatedDate());
    }
}
