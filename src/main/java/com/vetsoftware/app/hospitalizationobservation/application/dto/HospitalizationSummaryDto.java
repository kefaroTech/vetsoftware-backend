package com.vetsoftware.app.hospitalizationobservation.application.dto;

import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationRef;
import java.time.LocalDate;

public record HospitalizationSummaryDto(Long id, LocalDate date) {
  public static HospitalizationSummaryDto from(HospitalizationRef ref) {
    return new HospitalizationSummaryDto(ref.id(), ref.date());
  }
}
