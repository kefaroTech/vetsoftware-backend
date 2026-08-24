package com.vetsoftware.app.subscription.application.dto;

import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.CancellationRequest;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La cabecera del contrato tal como sale del caso de uso.
 *
 * <p>
 * {@code current} viaja calculado desde el dominio y no se deriva del
 * {@code status} en el front: el criterio de vigente incluye {@code PAST_DUE} y
 * {@code READ_ONLY}, y cada sitio que lo reescriba es un sitio que se puede
 * equivocar.
 */
public record SubscriptionDto(Long id, String subscriptionNumber, Long companyId, Long quoteId,
        Long priceListId, BillingCycle billingCycle, SubscriptionStatus status, boolean current,
        LocalDate startDate, LocalDate trialEndDate, LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd, LocalDate nextBillingDate, LocalDate commitmentEndDate,
        int graceDays, LocalDate pastDueSince, boolean autoRenew, LocalDateTime cancelRequestedAt,
        LocalDate cancelEffectiveDate, String cancelReason, LocalDateTime createdDate,
        boolean enabled) {

    public static SubscriptionDto from(Subscription subscription) {
        CancellationRequest cancellation = subscription.getCancellation();
        return new SubscriptionDto(subscription.getId(), subscription.getSubscriptionNumber(),
                subscription.getCompanyId(), subscription.getQuoteId(),
                subscription.getPriceListId(), subscription.getBillingCycle(),
                subscription.getStatus(), subscription.isCurrent(), subscription.getStartDate(),
                subscription.getTrialEndDate(), subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(), subscription.getNextBillingDate(),
                subscription.getCommitmentEndDate(), subscription.getGraceDays(),
                subscription.getPastDueSince(), subscription.isAutoRenew(),
                cancellation == null ? null : cancellation.requestedAt(),
                cancellation == null ? null : cancellation.effectiveDate(),
                cancellation == null ? null : cancellation.reason(), subscription.getCreatedDate(),
                subscription.isEnabled());
    }
}
