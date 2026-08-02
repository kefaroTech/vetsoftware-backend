package com.vetsoftware.app.deworming.domain;

public class DewormingNotFoundException extends RuntimeException {
  public DewormingNotFoundException(Long id) {
    super("Deworming not found: " + id);
  }
}
