package com.vetsoftware.app.prescription.domain;

public class PrescriptionNotFoundException extends RuntimeException {
  public PrescriptionNotFoundException(Long id) {
    super("Prescription not found: " + id);
  }
}
