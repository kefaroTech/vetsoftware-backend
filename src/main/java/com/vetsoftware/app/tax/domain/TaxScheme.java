package com.vetsoftware.app.tax.domain;

public enum TaxScheme {
  IVA("01"),
  INC("04");

  private final String dianCode;

  TaxScheme(String dianCode) {
    this.dianCode = dianCode;
  }

  public String dianCode() {
    return dianCode;
  }
}
