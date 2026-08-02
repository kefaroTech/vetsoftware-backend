package com.vetsoftware.app.diagnosticimagingtype.application.dto;

import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import java.time.LocalDateTime;

public record DiagnosticImagingTypeDto(
    Long id,
    String name,
    String description,
    CompanySummaryDto company,
    boolean general,
    LocalDateTime createdDate,
    boolean enabled) {
  public static DiagnosticImagingTypeDto from(DiagnosticImagingType type) {
    return new DiagnosticImagingTypeDto(
        type.getId(),
        type.getName(),
        type.getDescription(),
        type.getCompany() == null ? null : CompanySummaryDto.from(type.getCompany()),
        type.isGeneral(),
        type.getCreatedDate(),
        type.isEnabled());
  }
}
