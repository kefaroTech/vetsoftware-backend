package com.vetsoftware.app.productchargeopenaccount.application.dto;

import com.vetsoftware.app.productchargeopenaccount.domain.ProductRef;
import java.math.BigDecimal;

public record ProductSummaryDto(Long id, String name, String code, BigDecimal salePrice) {
  public static ProductSummaryDto from(ProductRef product) {
    return new ProductSummaryDto(product.id(), product.name(), product.code(), product.salePrice());
  }
}
