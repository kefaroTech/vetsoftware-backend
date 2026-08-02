package com.vetsoftware.app.consultation.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateConsultationCommand(Long id, LocalDate date, Long consultationTypeId,
        String anamnesis, String diagnosis, String prognosis, LocalDate nextControl, Long animalId,
        Long companyId, BigDecimal temperature, Integer heartRate, Integer respiratoryRate,
        String mucousMembranes, String capillaryRefill, String hydration,
        Integer bodyConditionScore, Integer painScore, String attitude, String examFindings) {
}
