package com.vetsoftware.app.electronicdocument.domain;

public class ElectronicDocumentNotFoundException extends RuntimeException {
  public ElectronicDocumentNotFoundException(Long id) {
    super("Electronic document not found: " + id);
  }
}
