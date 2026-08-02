package com.vetsoftware.app.diagnosticimaging.domain;

public class DiagnosticImagingNotFoundException extends RuntimeException {
  public DiagnosticImagingNotFoundException(Long id) {
    super("DiagnosticImaging not found: " + id);
  }
}
