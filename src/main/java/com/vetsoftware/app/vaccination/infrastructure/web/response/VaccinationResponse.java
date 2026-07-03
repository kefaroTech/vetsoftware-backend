package com.vetsoftware.app.vaccination.infrastructure.web.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VaccinationResponse(
        Long id,
        LocalDate date,
        VaccinationTypeSummary vaccinationType,
        String lot,
        String notes,
        String route,
        String applicationSite,
        LocalDate nextVaccination,
        AnimalSummary animal,
        ConsultationSummary consultation,
        CompanySummary company,
        LocalDateTime createdDate,
        boolean enabled
) {}
