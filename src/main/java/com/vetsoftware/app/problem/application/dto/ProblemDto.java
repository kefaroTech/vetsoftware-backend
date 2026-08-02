package com.vetsoftware.app.problem.application.dto;

import com.vetsoftware.app.problem.domain.Problem;
import com.vetsoftware.app.problem.domain.ProblemStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProblemDto(Long id, Long animalId, String animalName, String description,
        ProblemStatus status, LocalDate onsetDate, LocalDate resolvedDate, String notes,
        LocalDateTime createdDate, boolean enabled) {
    public static ProblemDto from(Problem problem) {
        return new ProblemDto(problem.getId(), problem.getAnimal().id(), problem.getAnimal().name(),
                problem.getDescription(), problem.getStatus(), problem.getOnsetDate(),
                problem.getResolvedDate(), problem.getNotes(), problem.getCreatedDate(),
                problem.isEnabled());
    }
}
