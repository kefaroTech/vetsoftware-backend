package com.vetsoftware.app.productcategory.domain;

public class ProductCategoryNotFoundException extends RuntimeException {
  public ProductCategoryNotFoundException(Long id) {
    super("ProductCategory not found: " + id);
  }
}
