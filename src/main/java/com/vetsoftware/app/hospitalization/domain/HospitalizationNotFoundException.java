package com.vetsoftware.app.hospitalization.domain;

public class HospitalizationNotFoundException extends RuntimeException {
  public HospitalizationNotFoundException(Long id) {
    super("Hospitalization not found: " + id);
  }
}
