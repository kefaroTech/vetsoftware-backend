package com.vetsoftware.app.vaccination.domain;

public record VaccinationTypeRef(Long id, String name) {
  public VaccinationTypeRef {
    if (id == null) throw new IllegalArgumentException("vaccination type id is required");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("vaccination type name is required");
  }
}
