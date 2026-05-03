package com.vetsoftware.app.laboratorytest.application.command;

import java.time.LocalDate;

public record CreateLaboratoryTestCommand(
        LocalDate date,
        Long testTypeId,
        Integer quantity,
        String diagnosis,
        Long animalId,
        Long companyId
) {}
