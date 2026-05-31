package com.vetsoftware.app.procedureschedule.infrastructure.web.response;

import java.time.LocalDateTime;

public record ProcedureScheduleResponse(
        Long id,
        HospitalizationProcedureSummary hospitalizationProcedure,
        LocalDateTime originalDateTime,
        LocalDateTime currentDateTime,
        LocalDateTime realDateTime,
        String appliedStatus,
        Boolean rescheduled,
        EmployeeSummary createdBy,
        LocalDateTime createdDate,
        boolean enabled
) {}
