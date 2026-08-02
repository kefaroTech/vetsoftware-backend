package com.vetsoftware.app.registration.domain;

/** El código de acceso (usuario) elegido en el registro ya está en uso. */
public class EmployeeCodeAlreadyExistsException extends RuntimeException {
  public EmployeeCodeAlreadyExistsException(String employeeCode) {
    super("Employee code already in use: " + employeeCode);
  }
}
