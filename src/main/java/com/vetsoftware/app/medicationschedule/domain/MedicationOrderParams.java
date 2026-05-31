package com.vetsoftware.app.medicationschedule.domain;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Datos de la orden de medicación que la generación del calendario necesita.
 * Companion VO propio de la feature (los enums llegan como String para no
 * acoplar con el dominio de hospitalizationmedication — vertical slicing).
 */
public record MedicationOrderParams(
        Long id,
        String name,
        Long hospitalizationId,
        String frequency,
        String guidelineType,
        String durationMeasure,
        Integer durationQuantity,
        LocalDate startDate,
        LocalTime startTime
) {
    public MedicationOrderParams {
        if (id == null) throw new IllegalArgumentException("medication id is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("medication name is required");
    }
}
