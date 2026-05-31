package com.vetsoftware.app.hospitalizationprocedure.application.dto;

import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedure;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record HospitalizationProcedureDto(
        Long id,
        String name,
        String dose,
        String frequency,
        String guidelineType,
        String durationMeasure,
        Integer durationQuantity,
        LocalDate startDate,
        LocalTime startTime,
        String notes,
        HospitalizationSummaryDto hospitalization,
        EmployeeSummaryDto createdBy,
        LocalDateTime createdDate,
        boolean enabled
) {
    public static HospitalizationProcedureDto from(HospitalizationProcedure procedure) {
        return new HospitalizationProcedureDto(
            procedure.getId(),
            procedure.getName(),
            procedure.getDose(),
            procedure.getFrequency() == null ? null : procedure.getFrequency().name(),
            procedure.getGuidelineType() == null ? null : procedure.getGuidelineType().name(),
            procedure.getDurationMeasure() == null ? null : procedure.getDurationMeasure().name(),
            procedure.getDurationQuantity(),
            procedure.getStartDate(),
            procedure.getStartTime(),
            procedure.getNotes(),
            HospitalizationSummaryDto.from(procedure.getHospitalization()),
            EmployeeSummaryDto.from(procedure.getCreatedBy()),
            procedure.getCreatedDate(),
            procedure.isEnabled());
    }
}
