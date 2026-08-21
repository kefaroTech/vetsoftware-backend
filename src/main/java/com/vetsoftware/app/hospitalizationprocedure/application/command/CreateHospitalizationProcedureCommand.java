package com.vetsoftware.app.hospitalizationprocedure.application.command;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * El {@code companyId} no viene del cuerpo de la peticion: lo pone el
 * controller desde el principal y el puerto de entrada lo revalida con
 * {@code @authz.isMyCompany}. Esta aqui porque sin el no hay con que acotar la
 * hospitalizacion padre, que es lo que impide colgar el procedimiento del
 * expediente de otro tenant.
 */
public record CreateHospitalizationProcedureCommand(String name, String dose, String frequency,
        String guidelineType, String durationMeasure, Integer durationQuantity, LocalDate startDate,
        LocalTime startTime, String notes, Long hospitalizationId, Long createdById,
        Long companyId) {
}
