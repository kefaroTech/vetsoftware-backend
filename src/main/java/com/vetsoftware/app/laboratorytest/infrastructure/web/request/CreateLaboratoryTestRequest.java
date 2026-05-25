package com.vetsoftware.app.laboratorytest.infrastructure.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateLaboratoryTestRequest(
        @NotNull LocalDate date,
        @NotNull Long testTypeId,
        @NotNull @Min(1) Integer quantity,
        @NotBlank @Size(max = 2000) String diagnosis,
        String status,
        @NotNull Long animalId,
        Long consultationId,
        @NotNull Long companyId
) {}
