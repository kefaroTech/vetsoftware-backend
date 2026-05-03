package com.vetsoftware.app.vaccinationtype.infrastructure.persistence;

import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
import org.springframework.stereotype.Component;

@Component
public class VaccinationTypeJpaMapper {
    public VaccinationTypeJpaEntity toJpa(VaccinationType vaccinationType) {
        VaccinationTypeJpaEntity entity = new VaccinationTypeJpaEntity();
        entity.setId(vaccinationType.getId());
        entity.setName(vaccinationType.getName());
        entity.setDescription(vaccinationType.getDescription());
        entity.setCreatedDate(vaccinationType.getCreatedDate());
        return entity;
    }

    public VaccinationType toDomain(VaccinationTypeJpaEntity entity) {
        return new VaccinationType(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedDate());
    }
}
