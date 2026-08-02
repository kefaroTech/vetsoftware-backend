package com.vetsoftware.app.deworming.application.command;

import com.vetsoftware.app.deworming.domain.DewormingType;
import java.time.LocalDate;

public record UpdateDewormingCommand(
    Long id,
    LocalDate date,
    LocalDate lastDeworming,
    DewormingType type,
    String product,
    String dosage,
    LocalDate nextControl,
    String observations,
    Long animalId,
    Long consultationId,
    Long companyId) {}
