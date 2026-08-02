package com.vetsoftware.app.hospitalizationobservation.domain;

public class HospitalizationObservationNotFoundException extends RuntimeException {
  public HospitalizationObservationNotFoundException(Long id) {
    super("HospitalizationObservation not found: " + id);
  }
}
