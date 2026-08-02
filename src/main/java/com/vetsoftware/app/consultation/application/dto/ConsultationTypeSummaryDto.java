package com.vetsoftware.app.consultation.application.dto;

import com.vetsoftware.app.consultation.domain.ConsultationTypeRef;

public record ConsultationTypeSummaryDto(Long id, String name) {
  public static ConsultationTypeSummaryDto from(ConsultationTypeRef ref) {
    return new ConsultationTypeSummaryDto(ref.id(), ref.name());
  }
}
