package com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence;

import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import org.springframework.stereotype.Component;

@Component
public class DiagnosticImagingTypeJpaMapper {
    public DiagnosticImagingTypeJpaEntity toJpa(DiagnosticImagingType type) {
        DiagnosticImagingTypeJpaEntity entity = new DiagnosticImagingTypeJpaEntity();
        entity.setId(type.getId());
        entity.setName(type.getName());
        entity.setDescription(type.getDescription());
        entity.setCreatedDate(type.getCreatedDate());
        return entity;
    }

    public DiagnosticImagingType toDomain(DiagnosticImagingTypeJpaEntity entity) {
        return new DiagnosticImagingType(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedDate());
    }
}
