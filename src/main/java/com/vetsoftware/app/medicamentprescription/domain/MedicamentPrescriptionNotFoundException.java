package com.vetsoftware.app.medicamentprescription.domain;

public class MedicamentPrescriptionNotFoundException extends RuntimeException {
  public MedicamentPrescriptionNotFoundException(Long id) {
    super("MedicamentPrescription not found: " + id);
  }
}
