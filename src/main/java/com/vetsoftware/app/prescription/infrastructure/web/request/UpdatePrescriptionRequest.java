package com.vetsoftware.app.prescription.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdatePrescriptionRequest(
        @NotNull LocalDate date,
        @NotBlank @Size(max = 2000) String diagnosis,
        @Size(max = 2000) String observations,
        @NotNull Long animalId,
        @NotNull Long consultationId
) {}
