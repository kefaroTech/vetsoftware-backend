package com.vetsoftware.app.spa.application.command;

import java.time.LocalDate;

public record UpdateSpaCommand(
    Long id,
    LocalDate date,
    Long spaTypeId,
    String reason,
    String details,
    String observations,
    Long animalId,
    Long companyId) {}
