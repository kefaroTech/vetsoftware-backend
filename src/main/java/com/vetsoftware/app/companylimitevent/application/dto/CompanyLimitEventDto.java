package com.vetsoftware.app.companylimitevent.application.dto;

import com.vetsoftware.app.companylimitevent.domain.CompanyLimitEvent;
import com.vetsoftware.app.companylimitevent.domain.LimitEventType;
import com.vetsoftware.app.companylimitevent.domain.LimitSource;
import java.time.LocalDateTime;

/** Un hecho de la bitácora tal como sale de la feature. */
public record CompanyLimitEventDto(Long id, Long companyId, Long limitDimensionId,
        LimitEventType eventType, int limitQuantity, int usedQuantity, int requestedDelta,
        LimitSource limitSource, Long overrideId, Long actorEmployeeId, Long actorSystemUserId,
        boolean actorIsProcess, String reasonCode, String reason, LocalDateTime occurredAt) {

    public static CompanyLimitEventDto from(CompanyLimitEvent event) {
        return new CompanyLimitEventDto(event.getId(), event.getCompanyId(),
                event.getLimitDimensionId(), event.getEventType(), event.getLimitQuantity(),
                event.getUsedQuantity(), event.getRequestedDelta(), event.getLimitSource(),
                event.getOverrideId(), event.getActor().employeeId(),
                event.getActor().systemUserId(), event.getActor().process(), event.getReasonCode(),
                event.getReason(), event.getOccurredAt());
    }
}
