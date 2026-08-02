package com.vetsoftware.app.specie.domain;

public class SpecieNotFoundException extends RuntimeException {
  public SpecieNotFoundException(Long id) {
    super("Specie not found: " + id);
  }
}
