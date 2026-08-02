package com.vetsoftware.app.hospitalizationprogressnote.infrastructure.web.response;

import java.time.LocalDateTime;

public record HospitalizationProgressNoteResponse(
    Long id,
    String description,
    HospitalizationSummary hospitalization,
    EmployeeSummary createdBy,
    LocalDateTime createdDate,
    boolean enabled) {}
