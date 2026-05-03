package com.vetsoftware.app.vaccination.application.command;

import java.time.LocalDate;

public record UpdateVaccinationCommand(
        Long id,
        LocalDate date,
        Long vaccinationTypeId,
        String lot,
        String notes,
        LocalDate nextVaccination,
        Long animalId,
        Long companyId
) {}
