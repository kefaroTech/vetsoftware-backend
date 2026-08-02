package com.vetsoftware.app.medicamentprescription.application.dto;

import com.vetsoftware.app.medicamentprescription.domain.PrescriptionRef;
import java.time.LocalDate;

public record PrescriptionSummaryDto(Long id, LocalDate date) {
  public static PrescriptionSummaryDto from(PrescriptionRef ref) {
    return new PrescriptionSummaryDto(ref.id(), ref.date());
  }
}
