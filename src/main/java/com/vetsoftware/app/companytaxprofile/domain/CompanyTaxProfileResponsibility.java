package com.vetsoftware.app.companytaxprofile.domain;

public record CompanyTaxProfileResponsibility(String code) {
  public CompanyTaxProfileResponsibility {
    if (code == null || code.isBlank())
      throw new IllegalArgumentException("responsibility code is required");
    if (code.length() > 10)
      throw new IllegalArgumentException("responsibility code must be 10 chars or less");
  }
}
