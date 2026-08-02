package com.vetsoftware.app.debtopenaccount.domain;

public record OpenAccountRef(Long id, Long companyId) {
  public OpenAccountRef {
    if (id == null) throw new IllegalArgumentException("open account id is required");
    if (companyId == null) throw new IllegalArgumentException("open account companyId is required");
  }
}
