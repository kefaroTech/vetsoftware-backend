package com.vetsoftware.app.surgery.application.dto;

import com.vetsoftware.app.surgery.domain.Surgery;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SurgeryDto(
        Long id,
        LocalDate date,
        SurgeryTypeSummaryDto surgeryType,
        String description,
        String medicament,
        String observations,
        String complications,
        AnimalSummaryDto animal,
        ConsultationSummaryDto consultation,
        CompanySummaryDto company,
        LocalDateTime createdDate
) {
    public static SurgeryDto from(Surgery surgery) {
        return new SurgeryDto(
            surgery.getId(),
            surgery.getDate(),
            SurgeryTypeSummaryDto.from(surgery.getSurgeryType()),
            surgery.getDescription(),
            surgery.getMedicament(),
            surgery.getObservations(),
            surgery.getComplications(),
            AnimalSummaryDto.from(surgery.getAnimal()),
            surgery.getConsultation() == null ? null : ConsultationSummaryDto.from(surgery.getConsultation()),
            CompanySummaryDto.from(surgery.getCompany()),
            surgery.getCreatedDate()
        );
    }
}
