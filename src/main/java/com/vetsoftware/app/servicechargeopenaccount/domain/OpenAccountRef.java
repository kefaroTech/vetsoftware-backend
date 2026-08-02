package com.vetsoftware.app.servicechargeopenaccount.domain;

public record OpenAccountRef(Long id, Long companyId) {
  public OpenAccountRef {
    if (id == null) throw new IllegalArgumentException("openAccount id is required");
    if (companyId == null) throw new IllegalArgumentException("openAccount companyId is required");
  }
}
