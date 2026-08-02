package com.vetsoftware.app.daycare.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.daycare.domain.AnimalRef;
import com.vetsoftware.app.daycare.domain.CompanyRef;
import com.vetsoftware.app.daycare.domain.DayCare;
import org.springframework.stereotype.Component;

@Component
public class DayCareJpaMapper {

  public DayCareJpaEntity toJpa(DayCare dayCare, AnimalJpaEntity animal, CompanyJpaEntity company) {
    DayCareJpaEntity entity = new DayCareJpaEntity();
    entity.setId(dayCare.getId());
    entity.setDate(dayCare.getDate());
    entity.setStartDate(dayCare.getStartDate());
    entity.setEndDate(dayCare.getEndDate());
    entity.setType(dayCare.getType());
    entity.setObjects(dayCare.getObjects());
    entity.setObservations(dayCare.getObservations());
    entity.setAnimal(animal);
    entity.setCompany(company);
    entity.setCreatedDate(dayCare.getCreatedDate());
    entity.setEnabled(dayCare.isEnabled());
    return entity;
  }

  public DayCare toDomain(DayCareJpaEntity entity) {
    AnimalJpaEntity a = entity.getAnimal();
    CompanyJpaEntity c = entity.getCompany();
    return toDomain(
        entity,
        new AnimalRef(a.getId(), a.getName(), a.getCode()),
        new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
  }

  public DayCare toDomain(DayCareJpaEntity entity, AnimalRef animalRef, CompanyRef companyRef) {
    return new DayCare(
        entity.getId(),
        entity.getDate(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getType(),
        entity.getObjects(),
        entity.getObservations(),
        animalRef,
        companyRef,
        entity.getCreatedDate(),
        entity.isEnabled());
  }
}
