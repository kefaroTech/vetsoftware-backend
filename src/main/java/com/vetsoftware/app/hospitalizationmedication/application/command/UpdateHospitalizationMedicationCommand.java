package com.vetsoftware.app.hospitalizationmedication.application.command;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateHospitalizationMedicationCommand(
    Long id,
    String name,
    String dose,
    String frequency,
    String guidelineType,
    String durationMeasure,
    Integer durationQuantity,
    LocalDate startDate,
    LocalTime startTime,
    String notes) {}
