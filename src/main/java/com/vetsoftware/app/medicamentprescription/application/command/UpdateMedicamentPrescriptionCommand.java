package com.vetsoftware.app.medicamentprescription.application.command;

public record UpdateMedicamentPrescriptionCommand(
        Long id,
        String name,
        String presentation,
        Double quantity,
        String posology,
        String observation,
        Long prescriptionId
) {}
