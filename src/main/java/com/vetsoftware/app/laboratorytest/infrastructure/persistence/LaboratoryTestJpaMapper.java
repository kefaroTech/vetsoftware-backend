package com.vetsoftware.app.laboratorytest.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.laboratorytest.domain.AnimalRef;
import com.vetsoftware.app.laboratorytest.domain.CompanyRef;
import com.vetsoftware.app.laboratorytest.domain.ConsultationRef;
import com.vetsoftware.app.laboratorytest.domain.EmployeeRef;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestPriority;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestStatus;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestTypeRef;
import com.vetsoftware.app.laboratorytesttype.infrastructure.persistence.LaboratoryTestTypeJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class LaboratoryTestJpaMapper {

    public LaboratoryTestJpaEntity toJpa(LaboratoryTest laboratoryTest,
            LaboratoryTestTypeJpaEntity testType, AnimalJpaEntity animal,
            ConsultationJpaEntity consultation, CompanyJpaEntity company,
            EmployeeJpaEntity processedBy) {
        LaboratoryTestJpaEntity entity = new LaboratoryTestJpaEntity();
        entity.setId(laboratoryTest.getId());
        entity.setDate(laboratoryTest.getDate());
        entity.setTestType(testType);
        entity.setQuantity(laboratoryTest.getQuantity());
        entity.setDiagnosis(laboratoryTest.getDiagnosis());
        entity.setStatus(laboratoryTest.getStatus().name());
        entity.setPrioridad(laboratoryTest.getPrioridad().name());
        entity.setAnimal(animal);
        entity.setConsultation(consultation);
        entity.setCompany(company);
        entity.setBranchId(laboratoryTest.getBranchId());
        entity.setProcessedBy(processedBy);
        entity.setProcessedDate(laboratoryTest.getProcessedDate());
        entity.setCreatedDate(laboratoryTest.getCreatedDate());
        entity.setVersion(laboratoryTest.getVersion());
        entity.setEnabled(laboratoryTest.isEnabled());
        return entity;
    }

    public LaboratoryTest toDomain(LaboratoryTestJpaEntity entity) {
        LaboratoryTestTypeJpaEntity tt = entity.getTestType();
        AnimalJpaEntity a = entity.getAnimal();
        ConsultationJpaEntity co = entity.getConsultation();
        CompanyJpaEntity c = entity.getCompany();
        EmployeeJpaEntity pb = entity.getProcessedBy();
        return toDomain(entity, new LaboratoryTestTypeRef(tt.getId(), tt.getName()),
                new AnimalRef(a.getId(), a.getName(), a.getCode()),
                co == null ? null : new ConsultationRef(co.getId(), co.getDate()),
                new CompanyRef(c.getId(), c.getName(), c.getIdentifier()),
                pb == null
                        ? null
                        : new EmployeeRef(pb.getId(), pb.getEmployeeCode(), pb.getName()));
    }

    public LaboratoryTest toDomain(LaboratoryTestJpaEntity entity,
            LaboratoryTestTypeRef testTypeRef, AnimalRef animalRef, ConsultationRef consultationRef,
            CompanyRef companyRef, EmployeeRef processedByRef) {
        return new LaboratoryTest(entity.getId(), entity.getDate(), testTypeRef,
                entity.getQuantity(), entity.getDiagnosis(),
                LaboratoryTestStatus.valueOf(entity.getStatus()),
                LaboratoryTestPriority.valueOf(entity.getPrioridad()), animalRef, consultationRef,
                companyRef, entity.getBranchId(), processedByRef, entity.getProcessedDate(),
                entity.getCreatedDate(), entity.getVersion(), entity.isEnabled());
    }
}
