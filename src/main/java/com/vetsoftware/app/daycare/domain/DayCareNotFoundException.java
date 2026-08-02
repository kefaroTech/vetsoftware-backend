package com.vetsoftware.app.daycare.domain;

public class DayCareNotFoundException extends RuntimeException {
  public DayCareNotFoundException(Long id) {
    super("DayCare not found: " + id);
  }
}
