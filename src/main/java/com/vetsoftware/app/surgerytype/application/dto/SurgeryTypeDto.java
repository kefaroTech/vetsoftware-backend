package com.vetsoftware.app.surgerytype.application.dto;

import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import java.time.LocalDateTime;

public record SurgeryTypeDto(Long id, String name, String description, LocalDateTime createdDate) {
    public static SurgeryTypeDto from(SurgeryType surgeryType) {
        return new SurgeryTypeDto(
                surgeryType.getId(),
                surgeryType.getName(),
                surgeryType.getDescription(),
                surgeryType.getCreatedDate());
    }
}
