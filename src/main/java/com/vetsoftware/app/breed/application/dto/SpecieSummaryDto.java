package com.vetsoftware.app.breed.application.dto;

import com.vetsoftware.app.breed.domain.SpecieRef;

public record SpecieSummaryDto(Long id, String name) {
  public static SpecieSummaryDto from(SpecieRef ref) {
    return new SpecieSummaryDto(ref.id(), ref.name());
  }
}
