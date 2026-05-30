package com.vetsoftware.app.laboratorytest.application.command;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CreateLaboratoryTestCommand(
        LocalDate date,
        Long testTypeId,
        Integer quantity,
        String diagnosis,
        String status,
        String prioridad,
        Long animalId,
        Long consultationId,
        Long companyId,
        Long processedById,
        LocalDateTime processedDate
) {}
