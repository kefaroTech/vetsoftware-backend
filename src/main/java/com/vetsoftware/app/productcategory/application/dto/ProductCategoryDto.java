package com.vetsoftware.app.productcategory.application.dto;

import com.vetsoftware.app.productcategory.domain.ProductCategory;
import java.time.LocalDateTime;

public record ProductCategoryDto(
    Long id,
    String name,
    String description,
    CompanySummaryDto company,
    LocalDateTime createdDate,
    LocalDateTime updatedDate,
    Long updatedBy,
    Long version,
    boolean enabled) {
  public static ProductCategoryDto from(ProductCategory productCategory) {
    return new ProductCategoryDto(
        productCategory.getId(),
        productCategory.getName(),
        productCategory.getDescription(),
        productCategory.getCompany() == null
            ? null
            : CompanySummaryDto.from(productCategory.getCompany()),
        productCategory.getCreatedDate(),
        productCategory.getUpdatedDate(),
        productCategory.getUpdatedBy(),
        productCategory.getVersion(),
        productCategory.isEnabled());
  }
}
