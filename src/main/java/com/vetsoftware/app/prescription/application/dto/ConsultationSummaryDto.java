package com.vetsoftware.app.prescription.application.dto;

import com.vetsoftware.app.prescription.domain.ConsultationRef;
import java.time.LocalDate;

public record ConsultationSummaryDto(Long id, LocalDate date) {
  public static ConsultationSummaryDto from(ConsultationRef ref) {
    return new ConsultationSummaryDto(ref.id(), ref.date());
  }
}
