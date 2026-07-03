package com.vetsoftware.app.problem.infrastructure.web.response;

import com.vetsoftware.app.problem.domain.ProblemStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProblemResponse(
        Long id,
        Long animalId,
        String animalName,
        String description,
        ProblemStatus status,
        LocalDate onsetDate,
        LocalDate resolvedDate,
        String notes,
        LocalDateTime createdDate,
        boolean enabled
) {}
