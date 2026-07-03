package com.vetsoftware.app.problem.application.command;

import com.vetsoftware.app.problem.domain.ProblemStatus;
import java.time.LocalDate;

public record UpdateProblemCommand(
        Long id,
        String description,
        ProblemStatus status,
        LocalDate onsetDate,
        LocalDate resolvedDate,
        String notes,
        Long companyId
) {}
