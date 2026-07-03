package com.vetsoftware.app.consultation.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

// weight/weightUnit son opcionales: si viene weight, se registra el peso del animal en la fecha de la
// consulta como punto de la serie temporal (source=CONSULTATION). Ver AnimalWeightPort.
// prognosis + campos de examen físico (temperature…examFindings) son la "O"/pronóstico de SOAP (Fase 3),
// todos opcionales; el service los ensambla en el VO PhysicalExam.
public record CreateConsultationCommand(
        LocalDate date,
        Long consultationTypeId,
        String anamnesis,
        String diagnosis,
        String therapeuticPlan,
        String diagnosisPlan,
        String prognosis,
        LocalDate nextControl,
        Long animalId,
        Long companyId,
        BigDecimal weight,
        String weightUnit,
        BigDecimal temperature,
        Integer heartRate,
        Integer respiratoryRate,
        String mucousMembranes,
        String capillaryRefill,
        String hydration,
        Integer bodyConditionScore,
        Integer painScore,
        String attitude,
        String examFindings
) {}
