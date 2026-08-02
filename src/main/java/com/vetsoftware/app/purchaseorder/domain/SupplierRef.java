package com.vetsoftware.app.purchaseorder.domain;

/** Companion VO: proveedor (feature {@code supplier}) al que se le emite la orden de compra. */
public record SupplierRef(Long id, String name) {
  public SupplierRef {
    if (id == null) throw new IllegalArgumentException("supplier id is required");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("supplier name is required");
  }
}
