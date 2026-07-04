package com.vetsoftware.app.medicamentprescription.application.command;

public record CreateMedicamentPrescriptionCommand(
        String name,
        String presentation,
        Double quantity,
        String posology,
        String observation,
        Long prescriptionId
) {}
