package com.vetsoftware.app.subscription.infrastructure.web.response;

import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * El contrato tal como sale por HTTP.
 *
 * <p>
 * {@code current} viaja calculado por el servidor: si el front dedujera vigente
 * de {@code status = ACTIVE}, un cliente en {@code PAST_DUE} —que debe pero
 * sigue trabajando— apareceria como sin contrato.
 */
public record SubscriptionResponse(Long id, String subscriptionNumber, Long companyId, Long quoteId,
        Long priceListId, BillingCycle billingCycle, SubscriptionStatus status, boolean current,
        LocalDate startDate, LocalDate trialEndDate, LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd, LocalDate nextBillingDate, LocalDate commitmentEndDate,
        int graceDays, LocalDate pastDueSince, boolean autoRenew, LocalDateTime cancelRequestedAt,
        LocalDate cancelEffectiveDate, String cancelReason, LocalDateTime createdDate,
        boolean enabled) {
}
