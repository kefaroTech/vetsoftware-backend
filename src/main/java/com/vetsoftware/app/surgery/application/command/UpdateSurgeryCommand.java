package com.vetsoftware.app.surgery.application.command;

import java.time.LocalDate;

public record UpdateSurgeryCommand(
        Long id,
        LocalDate date,
        Long surgeryTypeId,
        String description,
        String medicament,
        String observations,
        String complications,
        Long animalId,
        Long consultationId,
        Long companyId
) {}
