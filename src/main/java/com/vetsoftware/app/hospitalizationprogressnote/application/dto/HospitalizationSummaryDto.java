package com.vetsoftware.app.hospitalizationprogressnote.application.dto;

import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationRef;
import java.time.LocalDate;

public record HospitalizationSummaryDto(Long id, LocalDate date) {
  public static HospitalizationSummaryDto from(HospitalizationRef ref) {
    return new HospitalizationSummaryDto(ref.id(), ref.date());
  }
}
