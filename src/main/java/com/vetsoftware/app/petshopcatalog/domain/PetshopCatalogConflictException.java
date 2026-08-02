package com.vetsoftware.app.petshopcatalog.domain;

public class PetshopCatalogConflictException extends RuntimeException {
  private final String code;

  public PetshopCatalogConflictException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
