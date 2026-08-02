package com.vetsoftware.app.hospitalization.application.command;

import com.vetsoftware.app.hospitalization.domain.HospitalizationType;
import com.vetsoftware.app.hospitalization.domain.ReasonLeaving;
import java.math.BigDecimal;
import java.time.LocalDate;

// weight/weightUnit son opcionales: si viene weight, se registra el peso del animal en la fecha de
// la
// hospitalización como punto de la serie temporal (source=HOSPITALIZATION). Ver AnimalWeightPort.
public record CreateHospitalizationCommand(LocalDate date, LocalDate startDate, LocalDate endDate,
        HospitalizationType type, ReasonLeaving reasonLeaving, String reason, String observations,
        Long animalId, Long consultationId, Long companyId, BigDecimal weight, String weightUnit) {
}
