package com.vetsoftware.app.medicament.application.command;

public record CreateMedicamentCommand(String name, String description, Long companyId,
        boolean general) {
}
