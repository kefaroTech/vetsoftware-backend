package com.vetsoftware.app.purchaseorder.application.dto;

import com.vetsoftware.app.purchaseorder.domain.CompanyRef;

public record CompanySummaryDto(Long id, String name, String identifier) {
  public static CompanySummaryDto from(CompanyRef company) {
    return new CompanySummaryDto(company.id(), company.name(), company.identifier());
  }
}
