package com.vetsoftware.app.laboratorytestfile.application.dto;

import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestRef;
import java.time.LocalDate;

public record LaboratoryTestSummaryDto(Long id, LocalDate date) {
  public static LaboratoryTestSummaryDto from(LaboratoryTestRef ref) {
    return new LaboratoryTestSummaryDto(ref.id(), ref.date());
  }
}
