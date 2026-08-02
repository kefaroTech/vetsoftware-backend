package com.vetsoftware.app.laboratorytesttype.application.dto;

import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestType;
import java.time.LocalDateTime;

public record LaboratoryTestTypeDto(Long id, String name, String description,
        CompanySummaryDto company, boolean general, LocalDateTime createdDate, boolean enabled) {
    public static LaboratoryTestTypeDto from(LaboratoryTestType laboratoryTestType) {
        return new LaboratoryTestTypeDto(laboratoryTestType.getId(), laboratoryTestType.getName(),
                laboratoryTestType.getDescription(),
                laboratoryTestType.getCompany() == null
                        ? null
                        : CompanySummaryDto.from(laboratoryTestType.getCompany()),
                laboratoryTestType.isGeneral(), laboratoryTestType.getCreatedDate(),
                laboratoryTestType.isEnabled());
    }
}
