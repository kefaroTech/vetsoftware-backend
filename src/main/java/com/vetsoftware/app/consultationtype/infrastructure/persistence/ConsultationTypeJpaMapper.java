package com.vetsoftware.app.consultationtype.infrastructure.persistence;

import com.vetsoftware.app.consultationtype.domain.ConsultationType;
import org.springframework.stereotype.Component;

@Component
public class ConsultationTypeJpaMapper {
    public ConsultationTypeJpaEntity toJpa(ConsultationType consultationType) {
        ConsultationTypeJpaEntity entity = new ConsultationTypeJpaEntity();
        entity.setId(consultationType.getId());
        entity.setName(consultationType.getName());
        entity.setDescription(consultationType.getDescription());
        entity.setCreatedDate(consultationType.getCreatedDate());
        entity.setVersion(consultationType.getVersion());
        entity.setEnabled(consultationType.isEnabled());
        return entity;
    }

    public ConsultationType toDomain(ConsultationTypeJpaEntity entity) {
        return new ConsultationType(entity.getId(), entity.getName(), entity.getDescription(),
                entity.getCreatedDate(), entity.getVersion(), entity.isEnabled());
    }
}
