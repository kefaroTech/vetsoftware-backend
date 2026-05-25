package com.vetsoftware.app.surgery.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.surgery.domain.AnimalRef;
import com.vetsoftware.app.surgery.domain.CompanyRef;
import com.vetsoftware.app.surgery.domain.ConsultationRef;
import com.vetsoftware.app.surgery.domain.Surgery;
import com.vetsoftware.app.surgery.domain.SurgeryStatus;
import com.vetsoftware.app.surgery.domain.SurgeryTypeRef;
import com.vetsoftware.app.surgerytype.infrastructure.persistence.SurgeryTypeJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SurgeryJpaMapper {

    public SurgeryJpaEntity toJpa(Surgery surgery,
                                  SurgeryTypeJpaEntity surgeryType,
                                  AnimalJpaEntity animal,
                                  ConsultationJpaEntity consultation,
                                  CompanyJpaEntity company) {
        SurgeryJpaEntity entity = new SurgeryJpaEntity();
        entity.setId(surgery.getId());
        entity.setDate(surgery.getDate());
        entity.setSurgeryType(surgeryType);
        entity.setDescription(surgery.getDescription());
        entity.setMedicament(surgery.getMedicament());
        entity.setObservations(surgery.getObservations());
        entity.setComplications(surgery.getComplications());
        entity.setStatus(surgery.getStatus().name());
        entity.setAnimal(animal);
        entity.setConsultation(consultation);
        entity.setCompany(company);
        entity.setCreatedDate(surgery.getCreatedDate());
        entity.setEnabled(surgery.isEnabled());
        return entity;
    }

    public Surgery toDomain(SurgeryJpaEntity entity) {
        SurgeryTypeJpaEntity st = entity.getSurgeryType();
        AnimalJpaEntity a = entity.getAnimal();
        ConsultationJpaEntity co = entity.getConsultation();
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity,
            new SurgeryTypeRef(st.getId(), st.getName()),
            new AnimalRef(a.getId(), a.getName(), a.getCode()),
            co == null ? null : new ConsultationRef(co.getId(), co.getDate()),
            new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public Surgery toDomain(SurgeryJpaEntity entity, SurgeryTypeRef surgeryTypeRef,
                            AnimalRef animalRef, ConsultationRef consultationRef, CompanyRef companyRef) {
        return new Surgery(
            entity.getId(), entity.getDate(), surgeryTypeRef,
            entity.getDescription(), entity.getMedicament(), entity.getObservations(),
            entity.getComplications(),
            SurgeryStatus.valueOf(entity.getStatus()),
            animalRef, consultationRef, companyRef, entity.getCreatedDate(), entity.isEnabled());
    }
}
