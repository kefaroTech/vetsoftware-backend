package com.vetsoftware.app.diagnosticimagingtype.application.dto;

import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import java.time.LocalDateTime;

public record DiagnosticImagingTypeDto(Long id, String name, String description, LocalDateTime createdDate) {
    public static DiagnosticImagingTypeDto from(DiagnosticImagingType type) {
        return new DiagnosticImagingTypeDto(
                type.getId(),
                type.getName(),
                type.getDescription(),
                type.getCreatedDate());
    }
}
