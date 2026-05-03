package com.vetsoftware.app.consultation.application.command;

import java.time.LocalDate;

public record UpdateConsultationCommand(
        Long id,
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
