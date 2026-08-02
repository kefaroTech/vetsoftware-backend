package com.vetsoftware.app.consultation.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultation.domain.AnimalRef;
import com.vetsoftware.app.consultation.domain.CompanyRef;
import com.vetsoftware.app.consultation.domain.Consultation;
import com.vetsoftware.app.consultation.domain.ConsultationTypeRef;
import com.vetsoftware.app.consultation.domain.PhysicalExam;
import com.vetsoftware.app.consultationtype.infrastructure.persistence.ConsultationTypeJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ConsultationJpaMapper {

  public ConsultationJpaEntity toJpa(
      Consultation consultation,
      ConsultationTypeJpaEntity consultationType,
      AnimalJpaEntity animal,
      CompanyJpaEntity company) {
    ConsultationJpaEntity entity = new ConsultationJpaEntity();
    entity.setId(consultation.getId());
    entity.setDate(consultation.getDate());
    entity.setConsultationType(consultationType);
    entity.setAnamnesis(consultation.getAnamnesis());
    entity.setDiagnosis(consultation.getDiagnosis());
    entity.setPrognosis(consultation.getPrognosis());
    PhysicalExam exam = consultation.getPhysicalExam();
    entity.setTemperature(exam.temperature());
    entity.setHeartRate(exam.heartRate());
    entity.setRespiratoryRate(exam.respiratoryRate());
    entity.setMucousMembranes(exam.mucousMembranes());
    entity.setCapillaryRefill(exam.capillaryRefill());
    entity.setHydration(exam.hydration());
    entity.setBodyConditionScore(exam.bodyConditionScore());
    entity.setPainScore(exam.painScore());
    entity.setAttitude(exam.attitude());
    entity.setExamFindings(exam.findings());
    entity.setNextControl(consultation.getNextControl());
    entity.setAnimal(animal);
    entity.setCompany(company);
    entity.setCreatedDate(consultation.getCreatedDate());
    entity.setEnabled(consultation.isEnabled());
    return entity;
  }

  public Consultation toDomain(ConsultationJpaEntity entity) {
    ConsultationTypeJpaEntity ct = entity.getConsultationType();
    AnimalJpaEntity a = entity.getAnimal();
    CompanyJpaEntity c = entity.getCompany();
    return toDomain(
        entity,
        new ConsultationTypeRef(ct.getId(), ct.getName()),
        new AnimalRef(a.getId(), a.getName(), a.getCode()),
        new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
  }

  public Consultation toDomain(
      ConsultationJpaEntity entity,
      ConsultationTypeRef consultationTypeRef,
      AnimalRef animalRef,
      CompanyRef companyRef) {
    PhysicalExam exam =
        new PhysicalExam(
            entity.getTemperature(),
            entity.getHeartRate(),
            entity.getRespiratoryRate(),
            entity.getMucousMembranes(),
            entity.getCapillaryRefill(),
            entity.getHydration(),
            entity.getBodyConditionScore(),
            entity.getPainScore(),
            entity.getAttitude(),
            entity.getExamFindings());
    return new Consultation(
        entity.getId(),
        entity.getDate(),
        consultationTypeRef,
        entity.getAnamnesis(),
        entity.getDiagnosis(),
        entity.getPrognosis(),
        exam,
        entity.getNextControl(),
        animalRef,
        companyRef,
        entity.getCreatedDate(),
        entity.isEnabled());
  }
}
