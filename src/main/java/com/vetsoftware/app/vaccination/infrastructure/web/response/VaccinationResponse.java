package com.vetsoftware.app.vaccination.infrastructure.web.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VaccinationResponse(
        Long id,
        LocalDate date,
        VaccinationTypeSummary vaccinationType,
        String lot,
        String notes,
        LocalDate nextVaccination,
        AnimalSummary animal,
        CompanySummary company,
        LocalDateTime createdDate
) {}
