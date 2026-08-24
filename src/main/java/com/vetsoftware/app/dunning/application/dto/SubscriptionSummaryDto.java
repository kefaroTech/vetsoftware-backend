package com.vetsoftware.app.dunning.application.dto;

import com.vetsoftware.app.dunning.domain.SubscriptionRef;

public record SubscriptionSummaryDto(Long id, Long companyId, String subscriptionNumber,
        String status) {
    public static SubscriptionSummaryDto from(SubscriptionRef ref) {
        return new SubscriptionSummaryDto(ref.id(), ref.companyId(), ref.subscriptionNumber(),
                ref.status());
    }
}
