package com.vetsoftware.app.animalalert.domain;

public class AnimalAlertNotFoundException extends RuntimeException {
  public AnimalAlertNotFoundException(Long id) {
    super("AnimalAlert not found: " + id);
  }
}
