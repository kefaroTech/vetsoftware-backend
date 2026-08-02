package com.vetsoftware.app.vaccinationtype.application.command;

public record UpdateVaccinationTypeCommand(Long id, String name, String description, Long companyId,
        boolean general) {
}
