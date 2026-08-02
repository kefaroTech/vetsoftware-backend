package com.vetsoftware.app.openaccount.application.command;

public record UpdateOpenAccountCommand(
    Long id,
    Long ownerId,
    Long companyId,
    // Versión esperada de la cuenta (opt-in) para detección temprana de conflicto. null = sin
    // chequeo.
    Long expectedVersion) {}
