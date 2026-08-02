package com.vetsoftware.app.medicationschedule.application.dto;

import com.vetsoftware.app.medicationschedule.domain.MedicationSchedule;
import java.time.LocalDateTime;

public record MedicationScheduleDto(
    Long id,
    Long hospitalizationMedicationId,
    String hospitalizationMedicationName,
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
  public static MedicationScheduleDto from(MedicationSchedule s) {
    return new MedicationScheduleDto(
        s.getId(),
        s.getHospitalizationMedication().id(),
        s.getHospitalizationMedication().name(),
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
