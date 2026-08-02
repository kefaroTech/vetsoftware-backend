package com.vetsoftware.app.laboratorytesttype.application.dto;

import com.vetsoftware.app.laboratorytesttype.domain.CompanyRef;

public record CompanySummaryDto(Long id, String name, String identifier) {
  public static CompanySummaryDto from(CompanyRef ref) {
    return new CompanySummaryDto(ref.id(), ref.name(), ref.identifier());
  }
}
