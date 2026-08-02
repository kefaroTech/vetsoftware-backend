package com.vetsoftware.app.animal.application.dto;

import com.vetsoftware.app.animal.domain.AnimalColorRef;

public record AnimalColorSummaryDto(Long id, String name) {
  public static AnimalColorSummaryDto from(AnimalColorRef ref) {
    return new AnimalColorSummaryDto(ref.id(), ref.name());
  }
}
