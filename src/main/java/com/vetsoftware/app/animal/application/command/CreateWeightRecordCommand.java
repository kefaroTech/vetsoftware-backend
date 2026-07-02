package com.vetsoftware.app.animal.application.command;

import com.vetsoftware.app.animal.domain.WeightType;
import java.math.BigDecimal;
import java.time.LocalDate;

// unit y measuredAt son opcionales: si vienen null, el service usa la unidad preferida del animal y la
// fecha de hoy, respectivamente. source es siempre MANUAL para este caso de uso.
public record CreateWeightRecordCommand(
        Long animalId,
        BigDecimal value,
        WeightType unit,
        LocalDate measuredAt,
        String note,
        Long companyId
) {}
