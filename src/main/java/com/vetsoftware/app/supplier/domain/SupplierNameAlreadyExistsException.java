package com.vetsoftware.app.supplier.domain;

public class SupplierNameAlreadyExistsException extends RuntimeException {
  public SupplierNameAlreadyExistsException(String name) {
    super("Ya existe un proveedor activo con el nombre '" + name + "' en esta empresa.");
  }
}
