package com.vetsoftware.app.problem.infrastructure.web.request;

import com.vetsoftware.app.problem.domain.ProblemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateProblemRequest(@NotBlank @Size(max = 255) String description,
        @NotNull ProblemStatus status, LocalDate onsetDate, LocalDate resolvedDate,
        @Size(max = 2000) String notes) {
}
