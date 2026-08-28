package com.vetsoftware.app.companylimitevent.application.command;

import com.vetsoftware.app.companylimitevent.domain.EventActor;
import com.vetsoftware.app.companylimitevent.domain.LimitEventType;
import com.vetsoftware.app.companylimitevent.domain.LimitSource;

/** Escribir un hecho en la bitácora de cupo. */
public record RecordLimitEventCommand(Long companyId, Long limitDimensionId,
        LimitEventType eventType, int limitQuantity, int usedQuantity, int requestedDelta,
        LimitSource limitSource, Long overrideId, EventActor actor, String reasonCode,
        String reason) {
}
