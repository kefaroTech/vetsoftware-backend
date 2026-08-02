package com.vetsoftware.app.purchaseorder.domain;

/** Companion VO: producto (feature {@code product}) pedido en una línea de la orden de compra. */
public record ProductRef(Long id, String name, String code) {
  public ProductRef {
    if (id == null) throw new IllegalArgumentException("product id is required");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("product name is required");
    if (code == null || code.isBlank())
      throw new IllegalArgumentException("product code is required");
  }
}
