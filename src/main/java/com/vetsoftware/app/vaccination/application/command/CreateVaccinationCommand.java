package com.vetsoftware.app.vaccination.application.command;

import java.time.LocalDate;

public record CreateVaccinationCommand(
        LocalDate date,
        Long vaccinationTypeId,
        String lot,
        String notes,
        LocalDate nextVaccination,
        Long animalId,
        Long consultationId,
        Long companyId
) {}
