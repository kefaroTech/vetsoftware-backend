package com.vetsoftware.app.deworming.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.deworming.domain.AnimalRef;
import com.vetsoftware.app.deworming.domain.CompanyRef;
import com.vetsoftware.app.deworming.domain.Deworming;
import org.springframework.stereotype.Component;

@Component
public class DewormingJpaMapper {

    public DewormingJpaEntity toJpa(Deworming deworming, AnimalJpaEntity animal, CompanyJpaEntity company) {
        DewormingJpaEntity entity = new DewormingJpaEntity();
        entity.setId(deworming.getId());
        entity.setDate(deworming.getDate());
        entity.setLastDeworming(deworming.getLastDeworming());
        entity.setType(deworming.getType());
        entity.setProduct(deworming.getProduct());
        entity.setDosage(deworming.getDosage());
        entity.setNextControl(deworming.getNextControl());
        entity.setObservations(deworming.getObservations());
        entity.setAnimal(animal);
        entity.setCompany(company);
        entity.setCreatedDate(deworming.getCreatedDate());
        return entity;
    }

    public Deworming toDomain(DewormingJpaEntity entity) {
        AnimalJpaEntity a = entity.getAnimal();
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity,
            new AnimalRef(a.getId(), a.getName(), a.getCode()),
            new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public Deworming toDomain(DewormingJpaEntity entity, AnimalRef animalRef, CompanyRef companyRef) {
        return new Deworming(
            entity.getId(), entity.getDate(), entity.getLastDeworming(), entity.getType(),
            entity.getProduct(), entity.getDosage(), entity.getNextControl(),
            entity.getObservations(), animalRef, companyRef, entity.getCreatedDate());
    }
}
