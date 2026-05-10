package com.vetsoftware.app.vaccinationtype.application.dto;

import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
import java.time.LocalDateTime;

public record VaccinationTypeDto(
        Long id,
        String name,
        String description,
        CompanySummaryDto company,
        boolean general,
        LocalDateTime createdDate
) {
    public static VaccinationTypeDto from(VaccinationType vaccinationType) {
        return new VaccinationTypeDto(
                vaccinationType.getId(),
                vaccinationType.getName(),
                vaccinationType.getDescription(),
                vaccinationType.getCompany() == null ? null : CompanySummaryDto.from(vaccinationType.getCompany()),
                vaccinationType.isGeneral(),
                vaccinationType.getCreatedDate());
    }
}
