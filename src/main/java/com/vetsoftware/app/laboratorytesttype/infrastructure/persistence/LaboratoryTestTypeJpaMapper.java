package com.vetsoftware.app.laboratorytesttype.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.laboratorytesttype.domain.CompanyRef;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestType;
import org.springframework.stereotype.Component;

@Component
public class LaboratoryTestTypeJpaMapper {
  public LaboratoryTestTypeJpaEntity toJpa(
      LaboratoryTestType laboratoryTestType, CompanyJpaEntity company) {
    LaboratoryTestTypeJpaEntity entity = new LaboratoryTestTypeJpaEntity();
    entity.setId(laboratoryTestType.getId());
    entity.setName(laboratoryTestType.getName());
    entity.setDescription(laboratoryTestType.getDescription());
    entity.setCompany(company);
    entity.setGeneral(laboratoryTestType.isGeneral());
    entity.setCreatedDate(laboratoryTestType.getCreatedDate());
    entity.setEnabled(laboratoryTestType.isEnabled());
    return entity;
  }

  public LaboratoryTestType toDomain(LaboratoryTestTypeJpaEntity entity) {
    CompanyJpaEntity c = entity.getCompany();
    return toDomain(
        entity, c == null ? null : new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
  }

  public LaboratoryTestType toDomain(LaboratoryTestTypeJpaEntity entity, CompanyRef companyRef) {
    return new LaboratoryTestType(
        entity.getId(),
        entity.getName(),
        entity.getDescription(),
        companyRef,
        Boolean.TRUE.equals(entity.getGeneral()),
        entity.getCreatedDate(),
        entity.isEnabled());
  }
}
