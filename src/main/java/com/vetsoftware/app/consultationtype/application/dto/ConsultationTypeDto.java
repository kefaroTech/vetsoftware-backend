package com.vetsoftware.app.consultationtype.application.dto;

import com.vetsoftware.app.consultationtype.domain.ConsultationType;
import java.time.LocalDateTime;

public record ConsultationTypeDto(Long id, String name, String description, LocalDateTime createdDate) {
    public static ConsultationTypeDto from(ConsultationType consultationType) {
        return new ConsultationTypeDto(
                consultationType.getId(),
                consultationType.getName(),
                consultationType.getDescription(),
                consultationType.getCreatedDate());
    }
}
