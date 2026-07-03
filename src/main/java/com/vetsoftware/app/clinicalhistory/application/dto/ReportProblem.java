package com.vetsoftware.app.clinicalhistory.application.dto;

import java.time.LocalDate;

/** Ítem de la lista de problemas (POMR) del paciente, para el reporte PDF. */
public record ReportProblem(
        String description,
        String statusLabel,
        boolean active,
        LocalDate onsetDate,
        LocalDate resolvedDate,
        String notes
) {
}
