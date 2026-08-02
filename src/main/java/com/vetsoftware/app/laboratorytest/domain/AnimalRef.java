package com.vetsoftware.app.laboratorytest.domain;

public record AnimalRef(Long id, String name, String code) {
  public AnimalRef {
    if (id == null) throw new IllegalArgumentException("animal id is required");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("animal name is required");
  }
}
