package com.vetsoftware.app.employee.domain;

public class EmployeeNotFoundException extends RuntimeException {
  public EmployeeNotFoundException(Long id) {
    super("Employee not found: " + id);
  }
}
