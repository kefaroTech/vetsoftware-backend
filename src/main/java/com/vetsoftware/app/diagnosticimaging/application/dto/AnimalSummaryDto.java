package com.vetsoftware.app.diagnosticimaging.application.dto;

import com.vetsoftware.app.diagnosticimaging.domain.AnimalRef;

public record AnimalSummaryDto(Long id, String name, String code) {
  public static AnimalSummaryDto from(AnimalRef ref) {
    return new AnimalSummaryDto(ref.id(), ref.name(), ref.code());
  }
}
