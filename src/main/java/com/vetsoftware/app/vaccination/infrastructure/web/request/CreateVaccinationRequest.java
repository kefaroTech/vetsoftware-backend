package com.vetsoftware.app.vaccination.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateVaccinationRequest(
        @NotNull LocalDate date,
        @NotNull Long vaccinationTypeId,
        @NotBlank @Size(max = 100) String lot,
        @Size(max = 2000) String notes,
        // Vía de administración y sitio de aplicación (WSAVA) — opcionales.
        @Size(max = 30) String route,
        @Size(max = 60) String applicationSite,
        LocalDate nextVaccination,
        @NotNull Long animalId,
        Long consultationId
) {}
