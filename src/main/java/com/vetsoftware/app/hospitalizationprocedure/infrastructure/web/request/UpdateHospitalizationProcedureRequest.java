package com.vetsoftware.app.hospitalizationprocedure.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateHospitalizationProcedureRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 200) String dose,
        String frequency,
        String guidelineType,
        String durationMeasure,
        Integer durationQuantity,
        LocalDate startDate,
        LocalTime startTime,
        @Size(max = 2000) String notes
) {}
