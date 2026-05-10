package com.vetsoftware.app.spa.application.dto;

import com.vetsoftware.app.spa.domain.Spa;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SpaDto(
        Long id,
        LocalDate date,
        SpaTypeSummaryDto spaType,
        String reason,
        String details,
        String observations,
        AnimalSummaryDto animal,
        CompanySummaryDto company,
        LocalDateTime createdDate
) {
    public static SpaDto from(Spa spa) {
        return new SpaDto(
            spa.getId(),
            spa.getDate(),
            SpaTypeSummaryDto.from(spa.getSpaType()),
            spa.getReason(),
            spa.getDetails(),
            spa.getObservations(),
            AnimalSummaryDto.from(spa.getAnimal()),
            CompanySummaryDto.from(spa.getCompany()),
            spa.getCreatedDate()
        );
    }
}
