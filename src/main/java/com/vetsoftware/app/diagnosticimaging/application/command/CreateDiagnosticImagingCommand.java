package com.vetsoftware.app.diagnosticimaging.application.command;

import java.time.LocalDate;

public record CreateDiagnosticImagingCommand(
    LocalDate date,
    Long diagnosticImagingTypeId,
    String clinicalSigns,
    String studyType,
    String diagnosis,
    String observations,
    Long animalId,
    Long consultationId,
    Long companyId) {}
