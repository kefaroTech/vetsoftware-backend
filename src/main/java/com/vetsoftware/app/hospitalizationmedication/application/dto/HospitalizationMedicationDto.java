package com.vetsoftware.app.hospitalizationmedication.application.dto;

import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedication;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record HospitalizationMedicationDto(Long id, String name, String dose, String frequency,
        String guidelineType, String durationMeasure, Integer durationQuantity, LocalDate startDate,
        LocalTime startTime, String notes, HospitalizationSummaryDto hospitalization,
        EmployeeSummaryDto createdBy, LocalDateTime createdDate, boolean enabled,
        LocalDateTime suspensionDate, EmployeeSummaryDto suspensionBy) {
    public static HospitalizationMedicationDto from(HospitalizationMedication medication) {
        return new HospitalizationMedicationDto(medication.getId(), medication.getName(),
                medication.getDose(),
                medication.getFrequency() == null ? null : medication.getFrequency().name(),
                medication.getGuidelineType() == null ? null : medication.getGuidelineType().name(),
                medication.getDurationMeasure() == null
                        ? null
                        : medication.getDurationMeasure().name(),
                medication.getDurationQuantity(), medication.getStartDate(),
                medication.getStartTime(), medication.getNotes(),
                HospitalizationSummaryDto.from(medication.getHospitalization()),
                EmployeeSummaryDto.from(medication.getCreatedBy()), medication.getCreatedDate(),
                medication.isEnabled(), medication.getSuspensionDate(),
                medication.getSuspensionBy() == null
                        ? null
                        : EmployeeSummaryDto.from(medication.getSuspensionBy()));
    }
}
