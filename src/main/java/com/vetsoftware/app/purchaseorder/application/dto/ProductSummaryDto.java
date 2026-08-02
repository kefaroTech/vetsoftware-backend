package com.vetsoftware.app.purchaseorder.application.dto;

import com.vetsoftware.app.purchaseorder.domain.ProductRef;

public record ProductSummaryDto(Long id, String name, String code) {
  public static ProductSummaryDto from(ProductRef product) {
    return new ProductSummaryDto(product.id(), product.name(), product.code());
  }
}
