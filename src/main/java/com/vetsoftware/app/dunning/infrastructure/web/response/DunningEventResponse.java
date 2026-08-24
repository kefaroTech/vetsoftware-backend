package com.vetsoftware.app.dunning.infrastructure.web.response;

import com.vetsoftware.app.dunning.application.dto.BillingDocumentSummaryDto;
import com.vetsoftware.app.dunning.application.dto.DunningEventDto;
import com.vetsoftware.app.dunning.application.dto.SubscriptionSummaryDto;
import com.vetsoftware.app.dunning.domain.DunningChannel;
import com.vetsoftware.app.dunning.domain.DunningEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record DunningEventResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) DunningSubscriptionSummary subscription,
        DunningBillingDocumentSummary billingDocument,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) DunningEventType eventType,
        Integer daysOverdue, DunningChannel channel, String detail,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime occurredAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static DunningEventResponse from(DunningEventDto dto) {
        SubscriptionSummaryDto subscription = dto.subscription();
        BillingDocumentSummaryDto document = dto.billingDocument();
        return new DunningEventResponse(dto.id(), dto.companyId(),
                new DunningSubscriptionSummary(subscription.id(), subscription.companyId(),
                        subscription.subscriptionNumber(), subscription.status()),
                document == null
                        ? null
                        : new DunningBillingDocumentSummary(document.id(), document.companyId(),
                                document.documentNumber(), document.balanceAmount()),
                dto.eventType(), dto.daysOverdue(), dto.channel(), dto.detail(), dto.occurredAt(),
                dto.createdDate());
    }
}
