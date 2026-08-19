package com.vetsoftware.app.hospitalizationmedication.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import com.vetsoftware.app.hospitalizationmedication.domain.DurationMeasure;
import com.vetsoftware.app.hospitalizationmedication.domain.EmployeeRef;
import com.vetsoftware.app.hospitalizationmedication.domain.Frequency;
import com.vetsoftware.app.hospitalizationmedication.domain.GuidelineType;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedication;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationRef;
import org.springframework.stereotype.Component;

@Component
public class HospitalizationMedicationJpaMapper {

    public HospitalizationMedicationJpaEntity toJpa(HospitalizationMedication medication,
            HospitalizationJpaEntity hospitalization, EmployeeJpaEntity createdBy,
            EmployeeJpaEntity suspensionBy) {
        HospitalizationMedicationJpaEntity entity = new HospitalizationMedicationJpaEntity();
        entity.setId(medication.getId());
        entity.setName(medication.getName());
        entity.setDose(medication.getDose());
        entity.setFrequency(
                medication.getFrequency() == null ? null : medication.getFrequency().name());
        entity.setGuidelineType(medication.getGuidelineType() == null
                ? null
                : medication.getGuidelineType().name());
        entity.setDurationMeasure(medication.getDurationMeasure() == null
                ? null
                : medication.getDurationMeasure().name());
        entity.setDurationQuantity(medication.getDurationQuantity());
        entity.setStartDate(medication.getStartDate());
        entity.setStartTime(medication.getStartTime());
        entity.setNotes(medication.getNotes());
        entity.setHospitalization(hospitalization);
        entity.setCreatedBy(createdBy);
        entity.setCreatedDate(medication.getCreatedDate());
        entity.setVersion(medication.getVersion());
        entity.setEnabled(medication.isEnabled());
        entity.setSuspensionDate(medication.getSuspensionDate());
        entity.setSuspensionBy(suspensionBy);
        return entity;
    }

    // Read path — el @EntityGraph ya hidrató hospitalization y createdBy
    public HospitalizationMedication toDomain(HospitalizationMedicationJpaEntity entity) {
        HospitalizationJpaEntity h = entity.getHospitalization();
        EmployeeJpaEntity e = entity.getCreatedBy();
        EmployeeJpaEntity sb = entity.getSuspensionBy();
        EmployeeRef suspensionByRef = sb == null
                ? null
                : new EmployeeRef(sb.getId(), sb.getEmployeeCode(), sb.getName());
        return toDomain(entity, new HospitalizationRef(h.getId(), h.getDate()),
                new EmployeeRef(e.getId(), e.getEmployeeCode(), e.getName()), suspensionByRef);
    }

    // Write path — reusa los refs precargados, evita inicializar el proxy de
    // getReferenceById
    public HospitalizationMedication toDomain(HospitalizationMedicationJpaEntity entity,
            HospitalizationRef hospitalizationRef, EmployeeRef createdByRef,
            EmployeeRef suspensionByRef) {
        return new HospitalizationMedication(entity.getId(), entity.getName(), entity.getDose(),
                entity.getFrequency() == null ? null : Frequency.valueOf(entity.getFrequency()),
                entity.getGuidelineType() == null
                        ? null
                        : GuidelineType.valueOf(entity.getGuidelineType()),
                entity.getDurationMeasure() == null
                        ? null
                        : DurationMeasure.valueOf(entity.getDurationMeasure()),
                entity.getDurationQuantity(), entity.getStartDate(), entity.getStartTime(),
                entity.getNotes(), hospitalizationRef, createdByRef, entity.getCreatedDate(),
                entity.getVersion(), entity.isEnabled(), entity.getSuspensionDate(),
                suspensionByRef);
    }
}
