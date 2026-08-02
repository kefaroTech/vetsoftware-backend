package com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.diagnosticimagingtype.domain.CompanyRef;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import org.springframework.stereotype.Component;

@Component
public class DiagnosticImagingTypeJpaMapper {
  public DiagnosticImagingTypeJpaEntity toJpa(
      DiagnosticImagingType type, CompanyJpaEntity company) {
    DiagnosticImagingTypeJpaEntity entity = new DiagnosticImagingTypeJpaEntity();
    entity.setId(type.getId());
    entity.setName(type.getName());
    entity.setDescription(type.getDescription());
    entity.setCompany(company);
    entity.setGeneral(type.isGeneral());
    entity.setCreatedDate(type.getCreatedDate());
    entity.setEnabled(type.isEnabled());
    return entity;
  }

  public DiagnosticImagingType toDomain(DiagnosticImagingTypeJpaEntity entity) {
    CompanyJpaEntity c = entity.getCompany();
    return toDomain(
        entity, c == null ? null : new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
  }

  public DiagnosticImagingType toDomain(
      DiagnosticImagingTypeJpaEntity entity, CompanyRef companyRef) {
    return new DiagnosticImagingType(
        entity.getId(),
        entity.getName(),
        entity.getDescription(),
        companyRef,
        Boolean.TRUE.equals(entity.getGeneral()),
        entity.getCreatedDate(),
        entity.isEnabled());
  }
}
