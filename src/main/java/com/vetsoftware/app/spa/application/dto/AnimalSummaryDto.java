package com.vetsoftware.app.spa.application.dto;

import com.vetsoftware.app.spa.domain.AnimalRef;

public record AnimalSummaryDto(Long id, String name, String code) {
  public static AnimalSummaryDto from(AnimalRef ref) {
    return new AnimalSummaryDto(ref.id(), ref.name(), ref.code());
  }
}
