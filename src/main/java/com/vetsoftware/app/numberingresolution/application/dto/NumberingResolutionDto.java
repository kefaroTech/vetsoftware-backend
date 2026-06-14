package com.vetsoftware.app.numberingresolution.application.dto;

import com.vetsoftware.app.numberingresolution.domain.ElectronicDocumentType;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolution;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record NumberingResolutionDto(
        Long id,
        CompanySummaryDto company,
        ElectronicDocumentType documentType,
        String resolutionNumber,
        LocalDate resolutionDate,
        String prefix,
        Long rangeFrom,
        Long rangeTo,
        LocalDate validFrom,
        LocalDate validTo,
        String technicalKey,
        Long currentNumber,
        LocalDateTime createdDate,
        boolean enabled
) {
    public static NumberingResolutionDto from(NumberingResolution resolution) {
        return new NumberingResolutionDto(
                resolution.getId(),
                resolution.getCompany() == null ? null : CompanySummaryDto.from(resolution.getCompany()),
                resolution.getDocumentType(),
                resolution.getResolutionNumber(),
                resolution.getResolutionDate(),
                resolution.getPrefix(),
                resolution.getRangeFrom(),
                resolution.getRangeTo(),
                resolution.getValidFrom(),
                resolution.getValidTo(),
                resolution.getTechnicalKey(),
                resolution.getCurrentNumber(),
                resolution.getCreatedDate(),
                resolution.isEnabled());
    }
}
