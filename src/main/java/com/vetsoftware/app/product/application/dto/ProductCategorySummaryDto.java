package com.vetsoftware.app.product.application.dto;

import com.vetsoftware.app.product.domain.ProductCategoryRef;

public record ProductCategorySummaryDto(Long id, String name) {
  public static ProductCategorySummaryDto from(ProductCategoryRef productCategory) {
    return new ProductCategorySummaryDto(productCategory.id(), productCategory.name());
  }
}
