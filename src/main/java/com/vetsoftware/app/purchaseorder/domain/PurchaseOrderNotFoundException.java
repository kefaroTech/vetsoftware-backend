package com.vetsoftware.app.purchaseorder.domain;

public class PurchaseOrderNotFoundException extends RuntimeException {
  public PurchaseOrderNotFoundException(Long id) {
    super("Purchase order not found: " + id);
  }
}
