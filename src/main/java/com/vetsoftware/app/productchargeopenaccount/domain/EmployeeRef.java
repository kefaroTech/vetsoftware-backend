package com.vetsoftware.app.productchargeopenaccount.domain;

public record EmployeeRef(Long id, String name) {
  public EmployeeRef {
    if (id == null) throw new IllegalArgumentException("employee id is required");
  }
}
