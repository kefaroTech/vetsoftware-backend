package com.vetsoftware.app.consultation.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

// weight/weightUnit son opcionales: si viene weight, se registra el peso del animal en la fecha de la
// consulta como punto de la serie temporal (source=CONSULTATION). Ver AnimalWeightPort.
public record CreateConsultationCommand(
        LocalDate date,
        Long consultationTypeId,
        String anamnesis,
        String diagnosis,
        String therapeuticPlan,
        String diagnosisPlan,
        LocalDate nextControl,
        Long animalId,
        Long companyId,
        BigDecimal weight,
        String weightUnit
) {}
