package com.vetsoftware.app.deworming.application.dto;

import com.vetsoftware.app.deworming.domain.Deworming;
import com.vetsoftware.app.deworming.domain.DewormingType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DewormingDto(
        Long id,
        LocalDate date,
        LocalDate lastDeworming,
        DewormingType type,
        String product,
        String dosage,
        LocalDate nextControl,
        String observations,
        AnimalSummaryDto animal,
        ConsultationSummaryDto consultation,
        CompanySummaryDto company,
        LocalDateTime createdDate,
        boolean enabled
) {
    public static DewormingDto from(Deworming deworming) {
        return new DewormingDto(
            deworming.getId(),
            deworming.getDate(),
            deworming.getLastDeworming(),
            deworming.getType(),
            deworming.getProduct(),
            deworming.getDosage(),
            deworming.getNextControl(),
            deworming.getObservations(),
            AnimalSummaryDto.from(deworming.getAnimal()),
            deworming.getConsultation() == null ? null : ConsultationSummaryDto.from(deworming.getConsultation()),
            CompanySummaryDto.from(deworming.getCompany()),
            deworming.getCreatedDate(),
            deworming.isEnabled()
        );
    }
}
