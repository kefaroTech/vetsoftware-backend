package com.vetsoftware.app.subscription.application.dto;

import com.vetsoftware.app.subscription.domain.BillingCycle;
import java.util.List;

/** Proyección local del contrato comercial aceptado en quote. */
public record SubscriptionQuoteSnapshot(Long id, Long companyId, Long priceListId,
        BillingCycle billingCycle, boolean accepted, String acceptedBy,
        List<SubscriptionItemSnapshot> items) {
}
