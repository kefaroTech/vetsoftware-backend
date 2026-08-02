package com.vetsoftware.app.animal.domain;

public record OwnerRef(Long id, String name, String document) {
  public OwnerRef {
    if (id == null) throw new IllegalArgumentException("owner id is required");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("owner name is required");
    if (document == null || document.isBlank())
      throw new IllegalArgumentException("owner document is required");
  }
}
