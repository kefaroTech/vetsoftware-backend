package com.vetsoftware.app.medicament.domain;

public class MedicamentNotFoundException extends RuntimeException {
  public MedicamentNotFoundException(Long id) {
    super("Medicament not found: " + id);
  }
}
