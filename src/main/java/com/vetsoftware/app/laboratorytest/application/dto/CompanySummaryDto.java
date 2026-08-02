package com.vetsoftware.app.laboratorytest.application.dto;

import com.vetsoftware.app.laboratorytest.domain.CompanyRef;

public record CompanySummaryDto(Long id, String name, String identifier) {
  public static CompanySummaryDto from(CompanyRef ref) {
    return new CompanySummaryDto(ref.id(), ref.name(), ref.identifier());
  }
}
