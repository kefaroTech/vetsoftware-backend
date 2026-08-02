package com.vetsoftware.app.problem.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.problem.domain.AnimalRef;
import com.vetsoftware.app.problem.domain.CompanyRef;
import com.vetsoftware.app.problem.domain.Problem;
import org.springframework.stereotype.Component;

@Component
public class ProblemJpaMapper {

  public ProblemJpaEntity toJpa(Problem problem, AnimalJpaEntity animal, CompanyJpaEntity company) {
    ProblemJpaEntity entity = new ProblemJpaEntity();
    entity.setId(problem.getId());
    entity.setAnimal(animal);
    entity.setCompany(company);
    entity.setDescription(problem.getDescription());
    entity.setStatus(problem.getStatus());
    entity.setOnsetDate(problem.getOnsetDate());
    entity.setResolvedDate(problem.getResolvedDate());
    entity.setNotes(problem.getNotes());
    entity.setCreatedDate(problem.getCreatedDate());
    entity.setEnabled(problem.isEnabled());
    return entity;
  }

  public Problem toDomain(ProblemJpaEntity entity) {
    AnimalJpaEntity a = entity.getAnimal();
    CompanyJpaEntity c = entity.getCompany();
    return toDomain(
        entity,
        new AnimalRef(a.getId(), a.getName(), a.getCode()),
        new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
  }

  public Problem toDomain(ProblemJpaEntity entity, AnimalRef animalRef, CompanyRef companyRef) {
    return new Problem(
        entity.getId(),
        animalRef,
        companyRef,
        entity.getDescription(),
        entity.getStatus(),
        entity.getOnsetDate(),
        entity.getResolvedDate(),
        entity.getNotes(),
        entity.getCreatedDate(),
        entity.isEnabled());
  }
}
