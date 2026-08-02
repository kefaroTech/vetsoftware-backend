package com.vetsoftware.app.owner.application.dto;

import com.vetsoftware.app.owner.domain.CityRef;

public record CitySummaryDto(Long id, String name) {
  public static CitySummaryDto from(CityRef ref) {
    return new CitySummaryDto(ref.id(), ref.name());
  }
}
