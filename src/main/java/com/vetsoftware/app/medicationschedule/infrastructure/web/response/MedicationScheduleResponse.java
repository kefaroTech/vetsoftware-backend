package com.vetsoftware.app.medicationschedule.infrastructure.web.response;

import java.time.LocalDateTime;

public record MedicationScheduleResponse(Long id,
        HospitalizationMedicationSummary hospitalizationMedication, LocalDateTime originalDateTime,
        LocalDateTime currentDateTime, LocalDateTime realDateTime, String appliedStatus,
        Boolean rescheduled, EmployeeSummary createdBy, LocalDateTime createdDate,
        boolean enabled) {
}
