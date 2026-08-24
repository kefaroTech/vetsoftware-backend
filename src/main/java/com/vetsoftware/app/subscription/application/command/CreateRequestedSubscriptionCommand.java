package com.vetsoftware.app.subscription.application.command;

import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * Alta solicitada desde HTTP; contiene selección, nunca snapshots comerciales.
 */
public record CreateRequestedSubscriptionCommand(Long companyId, Long quoteId, Long priceListId,
        BillingCycle billingCycle, SubscriptionStatus status, LocalDate startDate,
        LocalDate trialEndDate, LocalDate currentPeriodStart, LocalDate currentPeriodEnd,
        LocalDate nextBillingDate, LocalDate commitmentEndDate, Integer graceDays,
        Boolean autoRenew, List<RequestedSubscriptionItemCommand> items) {
}
