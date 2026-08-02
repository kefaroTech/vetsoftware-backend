package com.vetsoftware.app.company.application.dto;

import com.vetsoftware.app.company.domain.CityRef;

public record CitySummaryDto(Long id, String name) {
  public static CitySummaryDto from(CityRef ref) {
    return new CitySummaryDto(ref.id(), ref.name());
  }
}
