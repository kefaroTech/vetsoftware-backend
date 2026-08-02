package com.vetsoftware.app.medicationschedule.domain;

public record HospitalizationMedicationRef(Long id, String name) {
  public HospitalizationMedicationRef {
    if (id == null) throw new IllegalArgumentException("hospitalization medication id is required");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("hospitalization medication name is required");
  }
}
