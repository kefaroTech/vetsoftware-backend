package com.vetsoftware.app.employee.domain;

public class AdminEmployeeCannotBeDisabledException extends RuntimeException {
  public AdminEmployeeCannotBeDisabledException(Long id) {
    super("Cannot disable employee " + id + ": has ADMIN role");
  }
}
