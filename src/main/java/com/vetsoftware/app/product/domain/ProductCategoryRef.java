package com.vetsoftware.app.product.domain;

public record ProductCategoryRef(Long id, String name) {
  public ProductCategoryRef {
    if (id == null) throw new IllegalArgumentException("productCategory id is required");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("productCategory name is required");
  }
}
