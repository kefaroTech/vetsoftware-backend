package com.vetsoftware.app.dunning.application.dto;

import com.vetsoftware.app.dunning.domain.DunningChannel;
import com.vetsoftware.app.dunning.domain.DunningEvent;
import com.vetsoftware.app.dunning.domain.DunningEventType;
import java.time.LocalDateTime;

public record DunningEventDto(Long id, Long companyId, SubscriptionSummaryDto subscription,
        BillingDocumentSummaryDto billingDocument, DunningEventType eventType, Integer daysOverdue,
        DunningChannel channel, String detail, LocalDateTime occurredAt,
        LocalDateTime createdDate) {

    public static DunningEventDto from(DunningEvent event) {
        return new DunningEventDto(event.getId(), event.getCompanyId(),
                SubscriptionSummaryDto.from(event.getSubscription()),
                event.getBillingDocument() == null
                        ? null
                        : BillingDocumentSummaryDto.from(event.getBillingDocument()),
                event.getEventType(), event.getDaysOverdue(), event.getChannel(), event.getDetail(),
                event.getOccurredAt(), event.getCreatedDate());
    }
}
