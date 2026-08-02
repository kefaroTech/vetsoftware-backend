package com.vetsoftware.app.animal.application.dto;

import com.vetsoftware.app.animal.domain.BreedRef;

public record BreedSummaryDto(Long id, String name) {
  public static BreedSummaryDto from(BreedRef ref) {
    return new BreedSummaryDto(ref.id(), ref.name());
  }
}
