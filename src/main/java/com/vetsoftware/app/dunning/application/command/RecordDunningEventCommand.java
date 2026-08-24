package com.vetsoftware.app.dunning.application.command;

import com.vetsoftware.app.dunning.domain.DunningChannel;
import com.vetsoftware.app.dunning.domain.DunningEventType;
import java.time.LocalDateTime;

public record RecordDunningEventCommand(Long companyId, Long subscriptionId, Long billingDocumentId,
        DunningEventType eventType, Integer daysOverdue, DunningChannel channel, String detail,
        LocalDateTime occurredAt) {
}
