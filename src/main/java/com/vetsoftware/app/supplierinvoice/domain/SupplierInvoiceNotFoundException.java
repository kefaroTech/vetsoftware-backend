package com.vetsoftware.app.supplierinvoice.domain;

public class SupplierInvoiceNotFoundException extends RuntimeException {
  public SupplierInvoiceNotFoundException(Long id) {
    super("Supplier invoice not found: " + id);
  }
}
