package com.vetsoftware.app.consultation.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateConsultationRequest(
        @NotNull LocalDate date,
        @NotNull Long consultationTypeId,
        @NotBlank @Size(max = 2000) String anamnesis,
        @NotBlank @Size(max = 2000) String diagnosis,
        @NotBlank @Size(max = 2000) String therapeuticPlan,
        @NotBlank @Size(max = 2000) String diagnosisPlan,
        LocalDate nextControl,
        @NotNull Long animalId,
        @NotNull Long companyId
) {}
