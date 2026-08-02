package com.vetsoftware.app.vaccinationtype.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.vaccinationtype.domain.CompanyRef;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
import org.springframework.stereotype.Component;

@Component
public class VaccinationTypeJpaMapper {
  public VaccinationTypeJpaEntity toJpa(VaccinationType vaccinationType, CompanyJpaEntity company) {
    VaccinationTypeJpaEntity entity = new VaccinationTypeJpaEntity();
    entity.setId(vaccinationType.getId());
    entity.setName(vaccinationType.getName());
    entity.setDescription(vaccinationType.getDescription());
    entity.setCompany(company);
    entity.setGeneral(vaccinationType.isGeneral());
    entity.setCreatedDate(vaccinationType.getCreatedDate());
    entity.setEnabled(vaccinationType.isEnabled());
    return entity;
  }

  public VaccinationType toDomain(VaccinationTypeJpaEntity entity) {
    CompanyJpaEntity c = entity.getCompany();
    return toDomain(
        entity, c == null ? null : new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
  }

  public VaccinationType toDomain(VaccinationTypeJpaEntity entity, CompanyRef companyRef) {
    return new VaccinationType(
        entity.getId(),
        entity.getName(),
        entity.getDescription(),
        companyRef,
        Boolean.TRUE.equals(entity.getGeneral()),
        entity.getCreatedDate(),
        entity.isEnabled());
  }
}
