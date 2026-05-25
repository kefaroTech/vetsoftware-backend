package com.vetsoftware.app.spa.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.spa.domain.AnimalRef;
import com.vetsoftware.app.spa.domain.CompanyRef;
import com.vetsoftware.app.spa.domain.Spa;
import com.vetsoftware.app.spa.domain.SpaStatus;
import com.vetsoftware.app.spa.domain.SpaTypeRef;
import com.vetsoftware.app.spatype.infrastructure.persistence.SpaTypeJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SpaJpaMapper {

    public SpaJpaEntity toJpa(Spa spa,
                              SpaTypeJpaEntity spaType,
                              AnimalJpaEntity animal,
                              CompanyJpaEntity company) {
        SpaJpaEntity entity = new SpaJpaEntity();
        entity.setId(spa.getId());
        entity.setDate(spa.getDate());
        entity.setSpaType(spaType);
        entity.setReason(spa.getReason());
        entity.setDetails(spa.getDetails());
        entity.setObservations(spa.getObservations());
        entity.setStatus(spa.getStatus().name());
        entity.setAnimal(animal);
        entity.setCompany(company);
        entity.setCreatedDate(spa.getCreatedDate());
        entity.setEnabled(spa.isEnabled());
        return entity;
    }

    public Spa toDomain(SpaJpaEntity entity) {
        SpaTypeJpaEntity st = entity.getSpaType();
        AnimalJpaEntity a = entity.getAnimal();
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity,
            new SpaTypeRef(st.getId(), st.getName()),
            new AnimalRef(a.getId(), a.getName(), a.getCode()),
            new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public Spa toDomain(SpaJpaEntity entity, SpaTypeRef spaTypeRef,
                        AnimalRef animalRef, CompanyRef companyRef) {
        return new Spa(
            entity.getId(), entity.getDate(), spaTypeRef,
            entity.getReason(), entity.getDetails(), entity.getObservations(),
            SpaStatus.valueOf(entity.getStatus()),
            animalRef, companyRef, entity.getCreatedDate(), entity.isEnabled());
    }
}
