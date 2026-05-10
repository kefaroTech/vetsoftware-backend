package com.vetsoftware.app.laboratorytest.application.command;

import java.time.LocalDate;

public record UpdateLaboratoryTestCommand(
        Long id,
        LocalDate date,
        Long testTypeId,
        Integer quantity,
        String diagnosis,
        Long animalId,
        Long consultationId,
        Long companyId
) {}
