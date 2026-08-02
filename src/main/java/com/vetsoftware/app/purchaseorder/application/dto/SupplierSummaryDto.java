package com.vetsoftware.app.purchaseorder.application.dto;

import com.vetsoftware.app.purchaseorder.domain.SupplierRef;

public record SupplierSummaryDto(Long id, String name) {
  public static SupplierSummaryDto from(SupplierRef supplier) {
    return new SupplierSummaryDto(supplier.id(), supplier.name());
  }
}
