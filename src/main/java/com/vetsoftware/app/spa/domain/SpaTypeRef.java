package com.vetsoftware.app.spa.domain;

public record SpaTypeRef(Long id, String name) {
  public SpaTypeRef {
    if (id == null) throw new IllegalArgumentException("spa type id is required");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("spa type name is required");
  }
}
