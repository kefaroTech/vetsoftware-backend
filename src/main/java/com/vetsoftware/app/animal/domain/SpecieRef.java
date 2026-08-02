package com.vetsoftware.app.animal.domain;

public record SpecieRef(Long id, String name) {
  public SpecieRef {
    if (id == null) throw new IllegalArgumentException("specie id is required");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("specie name is required");
  }
}
