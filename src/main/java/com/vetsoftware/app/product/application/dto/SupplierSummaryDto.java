package com.vetsoftware.app.product.application.dto;

import com.vetsoftware.app.product.domain.SupplierRef;

public record SupplierSummaryDto(Long id, String name) {
  public static SupplierSummaryDto from(SupplierRef supplier) {
    return new SupplierSummaryDto(supplier.id(), supplier.name());
  }
}
