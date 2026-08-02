package com.vetsoftware.app.consultation.domain;

public record ConsultationTypeRef(Long id, String name) {
  public ConsultationTypeRef {
    if (id == null) throw new IllegalArgumentException("consultation type id is required");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("consultation type name is required");
  }
}
