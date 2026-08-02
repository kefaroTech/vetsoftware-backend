package com.vetsoftware.app.deworming.application.dto;

import com.vetsoftware.app.deworming.domain.AnimalRef;

public record AnimalSummaryDto(Long id, String name, String code) {
  public static AnimalSummaryDto from(AnimalRef ref) {
    return new AnimalSummaryDto(ref.id(), ref.name(), ref.code());
  }
}
