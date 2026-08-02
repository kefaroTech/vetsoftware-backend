package com.vetsoftware.app.medicamentprescription.application.command;

public record CreateMedicamentPrescriptionCommand(
    Long medicamentId,
    String presentation,
    Double quantity,
    String posology,
    String observation,
    Long prescriptionId) {}
