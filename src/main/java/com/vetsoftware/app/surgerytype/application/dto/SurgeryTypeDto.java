package com.vetsoftware.app.surgerytype.application.dto;

import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import java.time.LocalDateTime;

public record SurgeryTypeDto(
        Long id,
        String name,
        String description,
        CompanySummaryDto company,
        boolean general,
        LocalDateTime createdDate
) {
    public static SurgeryTypeDto from(SurgeryType surgeryType) {
        return new SurgeryTypeDto(
                surgeryType.getId(),
                surgeryType.getName(),
                surgeryType.getDescription(),
                surgeryType.getCompany() == null ? null : CompanySummaryDto.from(surgeryType.getCompany()),
                surgeryType.isGeneral(),
                surgeryType.getCreatedDate());
    }
}
