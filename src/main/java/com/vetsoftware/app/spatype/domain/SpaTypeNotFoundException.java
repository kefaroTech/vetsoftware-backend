package com.vetsoftware.app.spatype.domain;

public class SpaTypeNotFoundException extends RuntimeException {
  public SpaTypeNotFoundException(Long id) {
    super("SpaType not found: " + id);
  }
}
