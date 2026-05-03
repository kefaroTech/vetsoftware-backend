package com.vetsoftware.app.consultation.application.dto;

import com.vetsoftware.app.consultation.domain.Consultation;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ConsultationDto(
        Long id,
        LocalDate date,
        ConsultationTypeSummaryDto consultationType,
        String anamnesis,
        String diagnosis,
        String therapeuticPlan,
        String diagnosisPlan,
        LocalDate nextControl,
        AnimalSummaryDto animal,
        CompanySummaryDto company,
        LocalDateTime createdDate
) {
    public static ConsultationDto from(Consultation consultation) {
        return new ConsultationDto(
            consultation.getId(),
            consultation.getDate(),
            ConsultationTypeSummaryDto.from(consultation.getConsultationType()),
            consultation.getAnamnesis(),
            consultation.getDiagnosis(),
            consultation.getTherapeuticPlan(),
            consultation.getDiagnosisPlan(),
            consultation.getNextControl(),
            AnimalSummaryDto.from(consultation.getAnimal()),
            CompanySummaryDto.from(consultation.getCompany()),
            consultation.getCreatedDate()
        );
    }
}
