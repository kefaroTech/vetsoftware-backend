package com.vetsoftware.app.laboratorytest.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.laboratorytest.domain.AnimalRef;
import com.vetsoftware.app.laboratorytest.domain.CompanyRef;
import com.vetsoftware.app.laboratorytest.domain.ConsultationRef;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import com.vetsoftware.app.laboratorytest.domain.TestTypeRef;
import com.vetsoftware.app.testtype.infrastructure.persistence.TestTypeJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class LaboratoryTestJpaMapper {

    public LaboratoryTestJpaEntity toJpa(LaboratoryTest laboratoryTest,
                                         TestTypeJpaEntity testType,
                                         AnimalJpaEntity animal,
                                         ConsultationJpaEntity consultation,
                                         CompanyJpaEntity company) {
        LaboratoryTestJpaEntity entity = new LaboratoryTestJpaEntity();
        entity.setId(laboratoryTest.getId());
        entity.setDate(laboratoryTest.getDate());
        entity.setTestType(testType);
        entity.setQuantity(laboratoryTest.getQuantity());
        entity.setDiagnosis(laboratoryTest.getDiagnosis());
        entity.setAnimal(animal);
        entity.setConsultation(consultation);
        entity.setCompany(company);
        entity.setCreatedDate(laboratoryTest.getCreatedDate());
        return entity;
    }

    public LaboratoryTest toDomain(LaboratoryTestJpaEntity entity) {
        TestTypeJpaEntity tt = entity.getTestType();
        AnimalJpaEntity a = entity.getAnimal();
        ConsultationJpaEntity co = entity.getConsultation();
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity,
            new TestTypeRef(tt.getId(), tt.getName()),
            new AnimalRef(a.getId(), a.getName(), a.getCode()),
            co == null ? null : new ConsultationRef(co.getId(), co.getDate()),
            new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public LaboratoryTest toDomain(LaboratoryTestJpaEntity entity, TestTypeRef testTypeRef,
                                   AnimalRef animalRef, ConsultationRef consultationRef,
                                   CompanyRef companyRef) {
        return new LaboratoryTest(
            entity.getId(), entity.getDate(), testTypeRef,
            entity.getQuantity(), entity.getDiagnosis(),
            animalRef, consultationRef, companyRef, entity.getCreatedDate());
    }
}
