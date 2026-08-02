package com.vetsoftware.app.prescription.application.command;

import java.time.LocalDate;

public record CreatePrescriptionCommand(
    LocalDate date,
    String diagnosis,
    String observations,
    Long animalId,
    Long consultationId,
    Long companyId) {}
