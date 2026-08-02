package com.vetsoftware.app.supplierinvoice.application.dto;

import com.vetsoftware.app.supplierinvoice.domain.SupplierRef;

public record SupplierSummaryDto(Long id, String name, String taxId) {
  public static SupplierSummaryDto from(SupplierRef ref) {
    return new SupplierSummaryDto(ref.id(), ref.name(), ref.taxId());
  }
}
