package com.vetsoftware.app.diagnosticimaging.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.diagnosticimaging.domain.AnimalRef;
import com.vetsoftware.app.diagnosticimaging.domain.CompanyRef;
import com.vetsoftware.app.diagnosticimaging.domain.ConsultationRef;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImaging;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingStatus;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingTypeRef;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence.DiagnosticImagingTypeJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class DiagnosticImagingJpaMapper {

  public DiagnosticImagingJpaEntity toJpa(
      DiagnosticImaging imaging,
      DiagnosticImagingTypeJpaEntity type,
      AnimalJpaEntity animal,
      ConsultationJpaEntity consultation,
      CompanyJpaEntity company) {
    DiagnosticImagingJpaEntity entity = new DiagnosticImagingJpaEntity();
    entity.setId(imaging.getId());
    entity.setDate(imaging.getDate());
    entity.setDiagnosticImagingType(type);
    entity.setClinicalSigns(imaging.getClinicalSigns());
    entity.setStudyType(imaging.getStudyType());
    entity.setDiagnosis(imaging.getDiagnosis());
    entity.setObservations(imaging.getObservations());
    entity.setStatus(imaging.getStatus().name());
    entity.setAnimal(animal);
    entity.setConsultation(consultation);
    entity.setCompany(company);
    entity.setCreatedDate(imaging.getCreatedDate());
    entity.setEnabled(imaging.isEnabled());
    return entity;
  }

  public DiagnosticImaging toDomain(DiagnosticImagingJpaEntity entity) {
    DiagnosticImagingTypeJpaEntity t = entity.getDiagnosticImagingType();
    AnimalJpaEntity a = entity.getAnimal();
    ConsultationJpaEntity co = entity.getConsultation();
    CompanyJpaEntity c = entity.getCompany();
    return toDomain(
        entity,
        new DiagnosticImagingTypeRef(t.getId(), t.getName()),
        new AnimalRef(a.getId(), a.getName(), a.getCode()),
        co == null ? null : new ConsultationRef(co.getId(), co.getDate()),
        new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
  }

  public DiagnosticImaging toDomain(
      DiagnosticImagingJpaEntity entity,
      DiagnosticImagingTypeRef typeRef,
      AnimalRef animalRef,
      ConsultationRef consultationRef,
      CompanyRef companyRef) {
    return new DiagnosticImaging(
        entity.getId(),
        entity.getDate(),
        typeRef,
        entity.getClinicalSigns(),
        entity.getStudyType(),
        entity.getDiagnosis(),
        entity.getObservations(),
        DiagnosticImagingStatus.valueOf(entity.getStatus()),
        animalRef,
        consultationRef,
        companyRef,
        entity.getCreatedDate(),
        entity.isEnabled());
  }
}
