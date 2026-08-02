package com.vetsoftware.app.laboratorytestfile.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.laboratorytest.infrastructure.persistence.LaboratoryTestJpaEntity;
import com.vetsoftware.app.laboratorytestfile.domain.EmployeeRef;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFile;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestRef;
import org.springframework.stereotype.Component;

@Component
public class LaboratoryTestFileJpaMapper {

    public LaboratoryTestFileJpaEntity toJpa(LaboratoryTestFile file, EmployeeJpaEntity uploadedBy,
            LaboratoryTestJpaEntity laboratoryTest) {
        LaboratoryTestFileJpaEntity entity = new LaboratoryTestFileJpaEntity();
        entity.setId(file.getId());
        entity.setStorageKey(file.getStorageKey());
        entity.setBucket(file.getBucket());
        entity.setOriginalFileName(file.getOriginalFileName());
        entity.setContentType(file.getContentType());
        entity.setSizeBytes(file.getSizeBytes());
        entity.setETag(file.getETag());
        entity.setUploadedBy(uploadedBy);
        entity.setLaboratoryTest(laboratoryTest);
        entity.setCreatedDate(file.getCreatedDate());
        return entity;
    }

    // Read path — el @EntityGraph ya hidrató uploadedBy y laboratoryTest
    public LaboratoryTestFile toDomain(LaboratoryTestFileJpaEntity entity) {
        EmployeeJpaEntity e = entity.getUploadedBy();
        LaboratoryTestJpaEntity lt = entity.getLaboratoryTest();
        return toDomain(entity, new EmployeeRef(e.getId(), e.getEmployeeCode(), e.getName()),
                new LaboratoryTestRef(lt.getId(), lt.getDate()));
    }

    // Write path — reusa los refs precargados, evita inicializar el proxy de
    // getReferenceById
    public LaboratoryTestFile toDomain(LaboratoryTestFileJpaEntity entity,
            EmployeeRef uploadedByRef, LaboratoryTestRef laboratoryTestRef) {
        return new LaboratoryTestFile(entity.getId(), entity.getStorageKey(), entity.getBucket(),
                entity.getOriginalFileName(), entity.getContentType(), entity.getSizeBytes(),
                entity.getETag(), uploadedByRef, laboratoryTestRef, entity.getCreatedDate());
    }
}
