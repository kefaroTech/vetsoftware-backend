package com.vetsoftware.app.hospitalizationmedication.domain;

public class HospitalizationMedicationNotFoundException extends RuntimeException {
  public HospitalizationMedicationNotFoundException(Long id) {
    super("HospitalizationMedication not found: " + id);
  }
}
