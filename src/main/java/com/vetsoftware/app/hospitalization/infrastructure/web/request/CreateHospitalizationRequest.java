package com.vetsoftware.app.hospitalization.infrastructure.web.request;

import com.vetsoftware.app.hospitalization.domain.HospitalizationType;
import com.vetsoftware.app.hospitalization.domain.ReasonLeaving;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateHospitalizationRequest(
        @NotNull LocalDate date,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotNull HospitalizationType type,
        ReasonLeaving reasonLeaving,
        @NotBlank @Size(max = 500) String reason,
        @Size(max = 2000) String observations,
        @NotNull Long animalId,
        Long consultationId,
        @NotNull Long companyId
) {}
