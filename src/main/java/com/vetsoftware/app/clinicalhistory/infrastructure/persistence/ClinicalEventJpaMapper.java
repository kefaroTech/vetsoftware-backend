package com.vetsoftware.app.clinicalhistory.infrastructure.persistence;

import com.vetsoftware.app.clinicalhistory.domain.ClinicalEvent;
import org.springframework.stereotype.Component;

@Component
public class ClinicalEventJpaMapper {

    public ClinicalEvent toDomain(ClinicalEventViewJpaEntity entity) {
        return new ClinicalEvent(entity.getSourceId(), entity.getAnimalId(), entity.getCompanyId(),
                entity.getConsultationId(), entity.getEventDate(), entity.getEndDate(),
                entity.getEventType(), entity.getSummary());
    }
}
