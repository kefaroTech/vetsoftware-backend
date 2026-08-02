package com.vetsoftware.app.dianprovider.domain;

public class DianProviderConfigNotFoundException extends RuntimeException {
  public DianProviderConfigNotFoundException(Long companyId) {
    super("DIAN provider config not found for company: " + companyId);
  }
}
