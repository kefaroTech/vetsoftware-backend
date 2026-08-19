package com.vetsoftware.app.hospitalizationprogressnote.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import com.vetsoftware.app.hospitalizationprogressnote.domain.EmployeeRef;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNote;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationRef;
import org.springframework.stereotype.Component;

@Component
public class HospitalizationProgressNoteJpaMapper {

    public HospitalizationProgressNoteJpaEntity toJpa(HospitalizationProgressNote progressNote,
            HospitalizationJpaEntity hospitalization, EmployeeJpaEntity createdBy) {
        HospitalizationProgressNoteJpaEntity entity = new HospitalizationProgressNoteJpaEntity();
        entity.setId(progressNote.getId());
        entity.setDescription(progressNote.getDescription());
        entity.setHospitalization(hospitalization);
        entity.setCreatedBy(createdBy);
        entity.setCreatedDate(progressNote.getCreatedDate());
        entity.setVersion(progressNote.getVersion());
        entity.setEnabled(progressNote.isEnabled());
        return entity;
    }

    // Read path — el @EntityGraph ya hidrató hospitalization y createdBy
    public HospitalizationProgressNote toDomain(HospitalizationProgressNoteJpaEntity entity) {
        HospitalizationJpaEntity h = entity.getHospitalization();
        EmployeeJpaEntity e = entity.getCreatedBy();
        return toDomain(entity, new HospitalizationRef(h.getId(), h.getDate()),
                new EmployeeRef(e.getId(), e.getEmployeeCode(), e.getName()));
    }

    // Write path — reusa los refs precargados, evita inicializar el proxy de
    // getReferenceById
    public HospitalizationProgressNote toDomain(HospitalizationProgressNoteJpaEntity entity,
            HospitalizationRef hospitalizationRef, EmployeeRef createdByRef) {
        return new HospitalizationProgressNote(entity.getId(), entity.getDescription(),
                hospitalizationRef, createdByRef, entity.getCreatedDate(), entity.getVersion(),
                entity.isEnabled());
    }
}
