package com.vetsoftware.app.consultation.application.command;

import java.time.LocalDate;

public record CreateConsultationCommand(
        LocalDate date,
        Long consultationTypeId,
        String anamnesis,
        String diagnosis,
        String therapeuticPlan,
        String diagnosisPlan,
        LocalDate nextControl,
        Long animalId,
        Long companyId
) {}
