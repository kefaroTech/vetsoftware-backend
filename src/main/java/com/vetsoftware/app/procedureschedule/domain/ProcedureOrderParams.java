package com.vetsoftware.app.procedureschedule.domain;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Datos de la orden de procedimiento que la generación del calendario necesita.
 * Companion VO propio de la feature (los enums llegan como String para no
 * acoplar con el dominio de hospitalizationprocedure — vertical slicing).
 */
public record ProcedureOrderParams(Long id, String name, Long hospitalizationId, String frequency,
        String guidelineType, String durationMeasure, Integer durationQuantity, LocalDate startDate,
        LocalTime startTime) {
    public ProcedureOrderParams {
        if (id == null)
            throw new IllegalArgumentException("procedure id is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("procedure name is required");
    }
}
