package com.vetsoftware.app.diagnosticimagingtype.domain;

public class DiagnosticImagingTypeNotFoundException extends RuntimeException {
  public DiagnosticImagingTypeNotFoundException(Long id) {
    super("DiagnosticImagingType not found: " + id);
  }
}
