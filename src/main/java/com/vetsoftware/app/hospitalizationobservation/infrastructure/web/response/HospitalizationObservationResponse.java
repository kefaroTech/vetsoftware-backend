package com.vetsoftware.app.hospitalizationobservation.infrastructure.web.response;

import java.time.LocalDateTime;

public record HospitalizationObservationResponse(
        Long id,
        String description,
        HospitalizationSummary hospitalization,
        EmployeeSummary createdBy,
        LocalDateTime createdDate,
        boolean enabled
) {}
