package com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import com.vetsoftware.app.hospitalizationprocedure.domain.DurationMeasure;
import com.vetsoftware.app.hospitalizationprocedure.domain.EmployeeRef;
import com.vetsoftware.app.hospitalizationprocedure.domain.Frequency;
import com.vetsoftware.app.hospitalizationprocedure.domain.GuidelineType;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedure;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationRef;
import org.springframework.stereotype.Component;

@Component
public class HospitalizationProcedureJpaMapper {

    public HospitalizationProcedureJpaEntity toJpa(HospitalizationProcedure procedure,
                                                   HospitalizationJpaEntity hospitalization,
                                                   EmployeeJpaEntity createdBy) {
        HospitalizationProcedureJpaEntity entity = new HospitalizationProcedureJpaEntity();
        entity.setId(procedure.getId());
        entity.setName(procedure.getName());
        entity.setDose(procedure.getDose());
        entity.setFrequency(procedure.getFrequency() == null ? null : procedure.getFrequency().name());
        entity.setGuidelineType(procedure.getGuidelineType() == null ? null : procedure.getGuidelineType().name());
        entity.setDurationMeasure(procedure.getDurationMeasure() == null ? null : procedure.getDurationMeasure().name());
        entity.setDurationQuantity(procedure.getDurationQuantity());
        entity.setStartDate(procedure.getStartDate());
        entity.setStartTime(procedure.getStartTime());
        entity.setNotes(procedure.getNotes());
        entity.setHospitalization(hospitalization);
        entity.setCreatedBy(createdBy);
        entity.setCreatedDate(procedure.getCreatedDate());
        entity.setEnabled(procedure.isEnabled());
        return entity;
    }

    // Read path — el @EntityGraph ya hidrató hospitalization y createdBy
    public HospitalizationProcedure toDomain(HospitalizationProcedureJpaEntity entity) {
        HospitalizationJpaEntity h = entity.getHospitalization();
        EmployeeJpaEntity e = entity.getCreatedBy();
        return toDomain(entity,
            new HospitalizationRef(h.getId(), h.getDate()),
            new EmployeeRef(e.getId(), e.getEmployeeCode(), e.getName()));
    }

    // Write path — reusa los refs precargados, evita inicializar el proxy de getReferenceById
    public HospitalizationProcedure toDomain(HospitalizationProcedureJpaEntity entity,
                                             HospitalizationRef hospitalizationRef, EmployeeRef createdByRef) {
        return new HospitalizationProcedure(
            entity.getId(),
            entity.getName(),
            entity.getDose(),
            entity.getFrequency() == null ? null : Frequency.valueOf(entity.getFrequency()),
            entity.getGuidelineType() == null ? null : GuidelineType.valueOf(entity.getGuidelineType()),
            entity.getDurationMeasure() == null ? null : DurationMeasure.valueOf(entity.getDurationMeasure()),
            entity.getDurationQuantity(),
            entity.getStartDate(),
            entity.getStartTime(),
            entity.getNotes(),
            hospitalizationRef,
            createdByRef,
            entity.getCreatedDate(),
            entity.isEnabled());
    }
}
