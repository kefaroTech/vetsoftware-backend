package com.vetsoftware.app.prescription.application.dto;

import com.vetsoftware.app.prescription.domain.AnimalRef;

public record AnimalSummaryDto(Long id, String name, String code) {
  public static AnimalSummaryDto from(AnimalRef ref) {
    return new AnimalSummaryDto(ref.id(), ref.name(), ref.code());
  }
}
