package com.vetsoftware.app.supplier.domain;

public class SupplierNotFoundException extends RuntimeException {
  public SupplierNotFoundException(Long id) {
    super("Supplier not found: " + id);
  }
}
