package com.vetsoftware.app.laboratorytest.application.dto;

import com.vetsoftware.app.laboratorytest.domain.ConsultationRef;
import java.time.LocalDate;

public record ConsultationSummaryDto(Long id, LocalDate date) {
  public static ConsultationSummaryDto from(ConsultationRef ref) {
    return new ConsultationSummaryDto(ref.id(), ref.date());
  }
}
