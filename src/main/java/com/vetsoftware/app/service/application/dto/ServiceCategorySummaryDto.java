package com.vetsoftware.app.service.application.dto;

import com.vetsoftware.app.service.domain.ServiceCategoryRef;

public record ServiceCategorySummaryDto(Long id, String name) {
  public static ServiceCategorySummaryDto from(ServiceCategoryRef ref) {
    return new ServiceCategorySummaryDto(ref.id(), ref.name());
  }
}
