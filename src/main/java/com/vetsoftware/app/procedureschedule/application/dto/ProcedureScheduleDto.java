package com.vetsoftware.app.procedureschedule.application.dto;

import com.vetsoftware.app.procedureschedule.domain.ProcedureSchedule;
import java.time.LocalDateTime;

public record ProcedureScheduleDto(
    Long id,
    Long hospitalizationProcedureId,
    String hospitalizationProcedureName,
    LocalDateTime originalDateTime,
    LocalDateTime currentDateTime,
    LocalDateTime realDateTime,
    String appliedStatus,
    Boolean rescheduled,
    Long createdById,
    String createdByCode,
    String createdByName,
    LocalDateTime createdDate,
    boolean enabled) {
  public static ProcedureScheduleDto from(ProcedureSchedule s) {
    return new ProcedureScheduleDto(
        s.getId(),
        s.getHospitalizationProcedure().id(),
        s.getHospitalizationProcedure().name(),
        s.getOriginalDateTime(),
        s.getCurrentDateTime(),
        s.getRealDateTime(),
        s.getAppliedStatus() != null ? s.getAppliedStatus().name() : null,
        s.getRescheduled(),
        s.getCreatedBy().id(),
        s.getCreatedBy().employeeCode(),
        s.getCreatedBy().name(),
        s.getCreatedDate(),
        s.isEnabled());
  }
}
