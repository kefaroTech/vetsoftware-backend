package com.vetsoftware.app.state.application.dto;

import com.vetsoftware.app.state.domain.CountryRef;

public record CountrySummaryDto(Long id, String name) {
  public static CountrySummaryDto from(CountryRef ref) {
    return new CountrySummaryDto(ref.id(), ref.name());
  }
}
