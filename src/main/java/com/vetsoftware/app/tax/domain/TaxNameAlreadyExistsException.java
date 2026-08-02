package com.vetsoftware.app.tax.domain;

public class TaxNameAlreadyExistsException extends RuntimeException {
  public TaxNameAlreadyExistsException(String name) {
    super("Ya existe un impuesto activo con el nombre '" + name + "' en esta empresa.");
  }
}
