package com.vetsoftware.app.vaccination.application.command;

import java.time.LocalDate;

public record UpdateVaccinationCommand(Long id, LocalDate date, Long vaccinationTypeId, String lot,
        String notes, String route, String applicationSite, LocalDate nextVaccination,
        Long animalId, Long consultationId, Long companyId) {
}
