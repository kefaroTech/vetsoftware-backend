package com.vetsoftware.app.consultation.application.dto;

import com.vetsoftware.app.consultation.domain.Consultation;
import com.vetsoftware.app.consultation.domain.PhysicalExam;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ConsultationDto(
        Long id,
        LocalDate date,
        ConsultationTypeSummaryDto consultationType,
        String anamnesis,
        String diagnosis,
        String prognosis,
        LocalDate nextControl,
        AnimalSummaryDto animal,
        CompanySummaryDto company,
        LocalDateTime createdDate,
        boolean enabled,
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
) {
    public static ConsultationDto from(Consultation consultation) {
        PhysicalExam exam = consultation.getPhysicalExam();
        return new ConsultationDto(
            consultation.getId(),
            consultation.getDate(),
            ConsultationTypeSummaryDto.from(consultation.getConsultationType()),
            consultation.getAnamnesis(),
            consultation.getDiagnosis(),
            consultation.getPrognosis(),
            consultation.getNextControl(),
            AnimalSummaryDto.from(consultation.getAnimal()),
            CompanySummaryDto.from(consultation.getCompany()),
            consultation.getCreatedDate(),
            consultation.isEnabled(),
            exam.temperature(),
            exam.heartRate(),
            exam.respiratoryRate(),
            exam.mucousMembranes(),
            exam.capillaryRefill(),
            exam.hydration(),
            exam.bodyConditionScore(),
            exam.painScore(),
            exam.attitude(),
            exam.findings()
        );
    }
}
