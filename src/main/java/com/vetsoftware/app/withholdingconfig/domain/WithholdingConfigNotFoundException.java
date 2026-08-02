package com.vetsoftware.app.withholdingconfig.domain;

public class WithholdingConfigNotFoundException extends RuntimeException {
  public WithholdingConfigNotFoundException(Long companyId) {
    super("Withholding config not found for company: " + companyId);
  }
}
