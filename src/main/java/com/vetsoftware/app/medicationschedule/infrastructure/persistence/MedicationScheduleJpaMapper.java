package com.vetsoftware.app.medicationschedule.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.hospitalizationmedication.infrastructure.persistence.HospitalizationMedicationJpaEntity;
import com.vetsoftware.app.medicationschedule.domain.AppliedStatus;
import com.vetsoftware.app.medicationschedule.domain.EmployeeRef;
import com.vetsoftware.app.medicationschedule.domain.HospitalizationMedicationRef;
import com.vetsoftware.app.medicationschedule.domain.MedicationSchedule;
import org.springframework.stereotype.Component;

@Component
public class MedicationScheduleJpaMapper {

    public MedicationScheduleJpaEntity toJpa(MedicationSchedule medicationSchedule,
            HospitalizationMedicationJpaEntity hospitalizationMedication,
            EmployeeJpaEntity createdBy) {
        MedicationScheduleJpaEntity entity = new MedicationScheduleJpaEntity();
        entity.setId(medicationSchedule.getId());
        entity.setHospitalizationMedication(hospitalizationMedication);
        entity.setCreatedBy(createdBy);
        entity.setOriginalDateTime(medicationSchedule.getOriginalDateTime());
        entity.setCurrentDateTime(medicationSchedule.getCurrentDateTime());
        entity.setRealDateTime(medicationSchedule.getRealDateTime());
        entity.setAppliedStatus(medicationSchedule.getAppliedStatus() != null
                ? medicationSchedule.getAppliedStatus().name()
                : null);
        entity.setRescheduled(medicationSchedule.getRescheduled());
        entity.setCreatedDate(medicationSchedule.getCreatedDate());
        entity.setEnabled(medicationSchedule.isEnabled());
        return entity;
    }

    // Read path — el @EntityGraph ya hidrató hospitalizationMedication y createdBy
    public MedicationSchedule toDomain(MedicationScheduleJpaEntity entity) {
        HospitalizationMedicationJpaEntity m = entity.getHospitalizationMedication();
        EmployeeJpaEntity e = entity.getCreatedBy();
        return toDomain(entity, new HospitalizationMedicationRef(m.getId(), m.getName()),
                new EmployeeRef(e.getId(), e.getEmployeeCode(), e.getName()));
    }

    // Write path — reusa los refs precargados, evita inicializar el proxy de
    // getReferenceById
    public MedicationSchedule toDomain(MedicationScheduleJpaEntity entity,
            HospitalizationMedicationRef hospitalizationMedicationRef, EmployeeRef createdByRef) {
        return new MedicationSchedule(entity.getId(), hospitalizationMedicationRef,
                entity.getOriginalDateTime(), entity.getCurrentDateTime(), entity.getRealDateTime(),
                entity.getAppliedStatus() != null
                        ? AppliedStatus.valueOf(entity.getAppliedStatus())
                        : null,
                entity.getRescheduled(), createdByRef, entity.getCreatedDate(), entity.isEnabled());
    }
}
