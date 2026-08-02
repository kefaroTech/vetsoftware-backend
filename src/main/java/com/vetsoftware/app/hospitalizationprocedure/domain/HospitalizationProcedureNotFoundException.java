package com.vetsoftware.app.hospitalizationprocedure.domain;

public class HospitalizationProcedureNotFoundException extends RuntimeException {
  public HospitalizationProcedureNotFoundException(Long id) {
    super("HospitalizationProcedure not found: " + id);
  }
}
