package com.vetsoftware.app.medicationschedule.domain;

public record EmployeeRef(Long id, String employeeCode, String name) {
  public EmployeeRef {
    if (id == null) throw new IllegalArgumentException("employee id is required");
    if (employeeCode == null || employeeCode.isBlank())
      throw new IllegalArgumentException("employee code is required");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("employee name is required");
  }
}
