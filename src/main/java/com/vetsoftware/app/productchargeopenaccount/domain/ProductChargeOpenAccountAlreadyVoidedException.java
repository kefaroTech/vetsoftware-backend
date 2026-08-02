package com.vetsoftware.app.productchargeopenaccount.domain;

public class ProductChargeOpenAccountAlreadyVoidedException extends RuntimeException {
  public ProductChargeOpenAccountAlreadyVoidedException(Long id) {
    super("Product charge open account already voided: " + id);
  }
}
