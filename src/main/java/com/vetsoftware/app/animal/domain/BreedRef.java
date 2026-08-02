package com.vetsoftware.app.animal.domain;

public record BreedRef(Long id, String name) {
  public BreedRef {
    if (id == null) throw new IllegalArgumentException("breed id is required");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("breed name is required");
  }
}
