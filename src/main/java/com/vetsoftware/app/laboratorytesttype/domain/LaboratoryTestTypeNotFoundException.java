package com.vetsoftware.app.laboratorytesttype.domain;

public class LaboratoryTestTypeNotFoundException extends RuntimeException {
  public LaboratoryTestTypeNotFoundException(Long id) {
    super("LaboratoryTestType not found: " + id);
  }
}
