package com.vetsoftware.app.hospitalization.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.hospitalization.domain.AnimalRef;
import com.vetsoftware.app.hospitalization.domain.CompanyRef;
import com.vetsoftware.app.hospitalization.domain.Hospitalization;
import org.springframework.stereotype.Component;

@Component
public class HospitalizationJpaMapper {

    public HospitalizationJpaEntity toJpa(Hospitalization hospitalization,
                                          AnimalJpaEntity animal,
                                          CompanyJpaEntity company) {
        HospitalizationJpaEntity entity = new HospitalizationJpaEntity();
        entity.setId(hospitalization.getId());
        entity.setDate(hospitalization.getDate());
        entity.setStartDate(hospitalization.getStartDate());
        entity.setEndDate(hospitalization.getEndDate());
        entity.setType(hospitalization.getType());
        entity.setReasonLeaving(hospitalization.getReasonLeaving());
        entity.setReason(hospitalization.getReason());
        entity.setObservations(hospitalization.getObservations());
        entity.setAnimal(animal);
        entity.setCompany(company);
        entity.setCreatedDate(hospitalization.getCreatedDate());
        return entity;
    }

    public Hospitalization toDomain(HospitalizationJpaEntity entity) {
        AnimalJpaEntity a = entity.getAnimal();
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity,
            new AnimalRef(a.getId(), a.getName(), a.getCode()),
            new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public Hospitalization toDomain(HospitalizationJpaEntity entity, AnimalRef animalRef, CompanyRef companyRef) {
        return new Hospitalization(
            entity.getId(), entity.getDate(), entity.getStartDate(), entity.getEndDate(),
            entity.getType(), entity.getReasonLeaving(),
            entity.getReason(), entity.getObservations(),
            animalRef, companyRef, entity.getCreatedDate());
    }
}
