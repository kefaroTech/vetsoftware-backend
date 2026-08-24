package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.subscription.domain.CancellationRequest;
import com.vetsoftware.app.subscription.domain.Subscription;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/** Unico sitio que conoce a la vez el dominio del contrato y su entidad JPA. */
@Component
public class SubscriptionJpaMapper {

    private final Clock clock;

    public SubscriptionJpaMapper(Clock clock) {
        this.clock = clock;
    }

    public SubscriptionJpaEntity toJpa(Subscription subscription, CompanyJpaEntity company) {
        SubscriptionJpaEntity entity = new SubscriptionJpaEntity();
        entity.setId(subscription.getId());
        entity.setSubscriptionNumber(subscription.getSubscriptionNumber());
        entity.setCompany(company);
        entity.setQuoteId(subscription.getQuoteId());
        entity.setPriceListId(subscription.getPriceListId());
        entity.setBillingCycle(subscription.getBillingCycle());
        entity.setStatus(subscription.getStatus());
        entity.setStartDate(subscription.getStartDate());
        entity.setTrialEndDate(subscription.getTrialEndDate());
        entity.setCurrentPeriodStart(subscription.getCurrentPeriodStart());
        entity.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
        entity.setNextBillingDate(subscription.getNextBillingDate());
        entity.setCommitmentEndDate(subscription.getCommitmentEndDate());
        entity.setGraceDays(subscription.getGraceDays());
        entity.setPastDueSince(subscription.getPastDueSince());
        entity.setAutoRenew(subscription.isAutoRenew());
        CancellationRequest cancellation = subscription.getCancellation();
        entity.setCancelRequestedAt(cancellation == null ? null : cancellation.requestedAt());
        entity.setCancelEffectiveDate(cancellation == null ? null : cancellation.effectiveDate());
        entity.setCancelReason(cancellation == null ? null : cancellation.reason());
        entity.setCreatedDate(subscription.getCreatedDate() == null
                ? LocalDateTime.now(clock)
                : subscription.getCreatedDate());
        entity.setVersion(subscription.getVersion());
        entity.setEnabled(subscription.isEnabled());
        return entity;
    }

    public Subscription toDomain(SubscriptionJpaEntity entity) {
        // Las dos fechas de cancelacion van juntas o no va ninguna
        // (chk_subscriptions_cancel), asi que basta mirar una.
        CancellationRequest cancellation = entity.getCancelRequestedAt() == null
                ? null
                : new CancellationRequest(entity.getCancelRequestedAt(),
                        entity.getCancelEffectiveDate(), entity.getCancelReason());
        return new Subscription(entity.getId(), entity.getSubscriptionNumber(),
                entity.getCompany().getId(), entity.getQuoteId(), entity.getPriceListId(),
                entity.getBillingCycle(), entity.getStatus(), entity.getStartDate(),
                entity.getTrialEndDate(), entity.getCurrentPeriodStart(),
                entity.getCurrentPeriodEnd(), entity.getNextBillingDate(),
                entity.getCommitmentEndDate(), entity.getGraceDays(), entity.getPastDueSince(),
                entity.isAutoRenew(), cancellation, entity.getCreatedDate(), entity.getVersion(),
                entity.isEnabled());
    }
}
