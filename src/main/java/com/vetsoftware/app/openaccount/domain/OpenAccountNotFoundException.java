package com.vetsoftware.app.openaccount.domain;

public class OpenAccountNotFoundException extends RuntimeException {
  public OpenAccountNotFoundException(Long id) {
    super("OpenAccount not found: " + id);
  }
}
