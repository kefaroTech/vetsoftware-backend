package com.vetsoftware.app.procedureschedule.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence.HospitalizationProcedureJpaEntity;
import com.vetsoftware.app.procedureschedule.domain.AppliedStatus;
import com.vetsoftware.app.procedureschedule.domain.EmployeeRef;
import com.vetsoftware.app.procedureschedule.domain.HospitalizationProcedureRef;
import com.vetsoftware.app.procedureschedule.domain.ProcedureSchedule;
import org.springframework.stereotype.Component;

@Component
public class ProcedureScheduleJpaMapper {

  public ProcedureScheduleJpaEntity toJpa(
      ProcedureSchedule procedureSchedule,
      HospitalizationProcedureJpaEntity hospitalizationProcedure,
      EmployeeJpaEntity createdBy) {
    ProcedureScheduleJpaEntity entity = new ProcedureScheduleJpaEntity();
    entity.setId(procedureSchedule.getId());
    entity.setHospitalizationProcedure(hospitalizationProcedure);
    entity.setCreatedBy(createdBy);
    entity.setOriginalDateTime(procedureSchedule.getOriginalDateTime());
    entity.setCurrentDateTime(procedureSchedule.getCurrentDateTime());
    entity.setRealDateTime(procedureSchedule.getRealDateTime());
    entity.setAppliedStatus(
        procedureSchedule.getAppliedStatus() != null
            ? procedureSchedule.getAppliedStatus().name()
            : null);
    entity.setRescheduled(procedureSchedule.getRescheduled());
    entity.setCreatedDate(procedureSchedule.getCreatedDate());
    entity.setEnabled(procedureSchedule.isEnabled());
    return entity;
  }

  // Read path — el @EntityGraph ya hidrató hospitalizationProcedure y createdBy
  public ProcedureSchedule toDomain(ProcedureScheduleJpaEntity entity) {
    HospitalizationProcedureJpaEntity p = entity.getHospitalizationProcedure();
    EmployeeJpaEntity e = entity.getCreatedBy();
    return toDomain(
        entity,
        new HospitalizationProcedureRef(p.getId(), p.getName()),
        new EmployeeRef(e.getId(), e.getEmployeeCode(), e.getName()));
  }

  // Write path — reusa los refs precargados, evita inicializar el proxy de getReferenceById
  public ProcedureSchedule toDomain(
      ProcedureScheduleJpaEntity entity,
      HospitalizationProcedureRef hospitalizationProcedureRef,
      EmployeeRef createdByRef) {
    return new ProcedureSchedule(
        entity.getId(),
        hospitalizationProcedureRef,
        entity.getOriginalDateTime(),
        entity.getCurrentDateTime(),
        entity.getRealDateTime(),
        entity.getAppliedStatus() != null ? AppliedStatus.valueOf(entity.getAppliedStatus()) : null,
        entity.getRescheduled(),
        createdByRef,
        entity.getCreatedDate(),
        entity.isEnabled());
  }
}
