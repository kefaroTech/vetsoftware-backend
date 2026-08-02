package com.vetsoftware.app.city.application.dto;

import com.vetsoftware.app.city.domain.StateRef;

public record StateSummaryDto(Long id, String name) {
  public static StateSummaryDto from(StateRef ref) {
    return new StateSummaryDto(ref.id(), ref.name());
  }
}
