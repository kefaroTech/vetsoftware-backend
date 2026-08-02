package com.vetsoftware.app.employeerole.domain;

public record RoleRef(Long id, String name, String code) {
  public RoleRef {
    if (id == null) throw new IllegalArgumentException("role id is required");
    if (name == null || name.isBlank()) throw new IllegalArgumentException("role name is required");
    if (code == null || code.isBlank()) throw new IllegalArgumentException("role code is required");
  }
}
