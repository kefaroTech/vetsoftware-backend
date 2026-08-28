package com.vetsoftware.app.companylimitevent.infrastructure.persistence;

import com.vetsoftware.app.companylimitevent.domain.CompanyLimitEvent;
import com.vetsoftware.app.companylimitevent.domain.EventActor;
import com.vetsoftware.app.companylimitevent.domain.LimitEventType;
import com.vetsoftware.app.companylimitevent.domain.LimitSource;
import org.springframework.stereotype.Component;

/** El único sitio que conoce a la vez el hecho de dominio y su fila. */
@Component
public class CompanyLimitEventJpaMapper {

    public CompanyLimitEventJpaEntity toJpa(CompanyLimitEvent event) {
        CompanyLimitEventJpaEntity entity = new CompanyLimitEventJpaEntity();
        entity.setId(event.getId());
        entity.setCompanyId(event.getCompanyId());
        entity.setLimitDimensionId(event.getLimitDimensionId());
        entity.setEventType(event.getEventType().name());
        entity.setLimitQuantity(event.getLimitQuantity());
        entity.setUsedQuantity(event.getUsedQuantity());
        entity.setRequestedDelta(event.getRequestedDelta());
        entity.setLimitSource(event.getLimitSource().name());
        entity.setOverrideId(event.getOverrideId());
        entity.setActorEmployeeId(event.getActor().employeeId());
        entity.setActorSystemUserId(event.getActor().systemUserId());
        entity.setActorIsProcess(event.getActor().process());
        entity.setReasonCode(event.getReasonCode());
        entity.setReason(event.getReason());
        entity.setOccurredAt(event.getOccurredAt());
        entity.setCreatedDate(event.getCreatedDate());
        return entity;
    }

    public CompanyLimitEvent toDomain(CompanyLimitEventJpaEntity entity) {
        EventActor actor = new EventActor(entity.getActorEmployeeId(),
                entity.getActorSystemUserId(), entity.isActorIsProcess());
        return new CompanyLimitEvent(entity.getId(), entity.getCompanyId(),
                entity.getLimitDimensionId(), LimitEventType.valueOf(entity.getEventType()),
                entity.getLimitQuantity(), entity.getUsedQuantity(), entity.getRequestedDelta(),
                LimitSource.valueOf(entity.getLimitSource()), entity.getOverrideId(), actor,
                entity.getReasonCode(), entity.getReason(), entity.getOccurredAt(),
                entity.getCreatedDate());
    }
}
