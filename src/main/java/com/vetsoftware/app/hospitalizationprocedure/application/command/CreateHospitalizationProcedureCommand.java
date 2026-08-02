package com.vetsoftware.app.hospitalizationprocedure.application.command;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateHospitalizationProcedureCommand(String name, String dose, String frequency,
        String guidelineType, String durationMeasure, Integer durationQuantity, LocalDate startDate,
        LocalTime startTime, String notes, Long hospitalizationId, Long createdById) {
}
