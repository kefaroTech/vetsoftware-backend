package com.vetsoftware.app.tax.domain;

public class TaxNotFoundException extends RuntimeException {
  public TaxNotFoundException(Long id) {
    super("Tax not found: " + id);
  }
}
