package com.vetsoftware.app.surgerytype.infrastructure.persistence;

import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import org.springframework.stereotype.Component;

@Component
public class SurgeryTypeJpaMapper {
    public SurgeryTypeJpaEntity toJpa(SurgeryType surgeryType) {
        SurgeryTypeJpaEntity entity = new SurgeryTypeJpaEntity();
        entity.setId(surgeryType.getId());
        entity.setName(surgeryType.getName());
        entity.setDescription(surgeryType.getDescription());
        entity.setCreatedDate(surgeryType.getCreatedDate());
        return entity;
    }

    public SurgeryType toDomain(SurgeryTypeJpaEntity entity) {
        return new SurgeryType(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedDate());
    }
}
