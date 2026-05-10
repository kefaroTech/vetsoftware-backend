package com.vetsoftware.app.prescription.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.prescription.domain.AnimalRef;
import com.vetsoftware.app.prescription.domain.CompanyRef;
import com.vetsoftware.app.prescription.domain.ConsultationRef;
import com.vetsoftware.app.prescription.domain.Prescription;
import org.springframework.stereotype.Component;

@Component
public class PrescriptionJpaMapper {

    public PrescriptionJpaEntity toJpa(Prescription prescription, AnimalJpaEntity animal,
                                       ConsultationJpaEntity consultation, CompanyJpaEntity company) {
        PrescriptionJpaEntity entity = new PrescriptionJpaEntity();
        entity.setId(prescription.getId());
        entity.setDate(prescription.getDate());
        entity.setDiagnosis(prescription.getDiagnosis());
        entity.setObservations(prescription.getObservations());
        entity.setAnimal(animal);
        entity.setConsultation(consultation);
        entity.setCompany(company);
        entity.setCreatedDate(prescription.getCreatedDate());
        return entity;
    }

    public Prescription toDomain(PrescriptionJpaEntity entity) {
        AnimalJpaEntity a = entity.getAnimal();
        ConsultationJpaEntity co = entity.getConsultation();
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity,
            new AnimalRef(a.getId(), a.getName(), a.getCode()),
            new ConsultationRef(co.getId(), co.getDate()),
            new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public Prescription toDomain(PrescriptionJpaEntity entity, AnimalRef animalRef,
                                 ConsultationRef consultationRef, CompanyRef companyRef) {
        return new Prescription(
            entity.getId(), entity.getDate(),
            entity.getDiagnosis(), entity.getObservations(),
            animalRef, consultationRef, companyRef, entity.getCreatedDate());
    }
}
