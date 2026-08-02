package com.vetsoftware.app.diagnosticimaging.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateDiagnosticImagingRequest(@NotNull LocalDate date,
        @NotNull Long diagnosticImagingTypeId, @NotBlank @Size(max = 2000) String clinicalSigns,
        @NotBlank @Size(max = 200) String studyType, @NotBlank @Size(max = 2000) String diagnosis,
        @Size(max = 2000) String observations, @NotNull Long animalId, Long consultationId) {
}
