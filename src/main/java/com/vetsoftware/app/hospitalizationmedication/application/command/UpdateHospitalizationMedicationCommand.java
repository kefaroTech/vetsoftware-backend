package com.vetsoftware.app.hospitalizationmedication.application.command;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * El {@code companyId} no viaja en el request REST: lo pone el controller desde
 * el contexto autenticado y el {@code @PreAuthorize} del puerto lo revalida.
 */
public record UpdateHospitalizationMedicationCommand(Long id, String name, String dose,
        String frequency, String guidelineType, String durationMeasure, Integer durationQuantity,
        LocalDate startDate, LocalTime startTime, String notes, Long companyId) {
}
