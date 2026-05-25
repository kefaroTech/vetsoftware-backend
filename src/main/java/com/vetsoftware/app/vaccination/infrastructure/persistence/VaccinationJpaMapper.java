package com.vetsoftware.app.vaccination.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.vaccination.domain.AnimalRef;
import com.vetsoftware.app.vaccination.domain.CompanyRef;
import com.vetsoftware.app.vaccination.domain.ConsultationRef;
import com.vetsoftware.app.vaccination.domain.Vaccination;
import com.vetsoftware.app.vaccination.domain.VaccinationTypeRef;
import com.vetsoftware.app.vaccinationtype.infrastructure.persistence.VaccinationTypeJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class VaccinationJpaMapper {

    public VaccinationJpaEntity toJpa(Vaccination vaccination,
                                      VaccinationTypeJpaEntity vaccinationType,
                                      AnimalJpaEntity animal,
                                      ConsultationJpaEntity consultation,
                                      CompanyJpaEntity company) {
        VaccinationJpaEntity entity = new VaccinationJpaEntity();
        entity.setId(vaccination.getId());
        entity.setDate(vaccination.getDate());
        entity.setVaccinationType(vaccinationType);
        entity.setLot(vaccination.getLot());
        entity.setNotes(vaccination.getNotes());
        entity.setNextVaccination(vaccination.getNextVaccination());
        entity.setAnimal(animal);
        entity.setConsultation(consultation);
        entity.setCompany(company);
        entity.setCreatedDate(vaccination.getCreatedDate());
        entity.setEnabled(vaccination.isEnabled());
        return entity;
    }

    public Vaccination toDomain(VaccinationJpaEntity entity) {
        VaccinationTypeJpaEntity vt = entity.getVaccinationType();
        AnimalJpaEntity a = entity.getAnimal();
        ConsultationJpaEntity co = entity.getConsultation();
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity,
            new VaccinationTypeRef(vt.getId(), vt.getName()),
            new AnimalRef(a.getId(), a.getName(), a.getCode()),
            co == null ? null : new ConsultationRef(co.getId(), co.getDate()),
            new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public Vaccination toDomain(VaccinationJpaEntity entity, VaccinationTypeRef vaccinationTypeRef,
                                AnimalRef animalRef, ConsultationRef consultationRef, CompanyRef companyRef) {
        return new Vaccination(
            entity.getId(), entity.getDate(), vaccinationTypeRef,
            entity.getLot(), entity.getNotes(), entity.getNextVaccination(),
            animalRef, consultationRef, companyRef, entity.getCreatedDate(), entity.isEnabled());
    }
}
