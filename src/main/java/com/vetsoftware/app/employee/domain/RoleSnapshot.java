package com.vetsoftware.app.employee.domain;

public record RoleSnapshot(Long employeeRoleId, Long id, String name, String code) {
  public RoleSnapshot {
    if (employeeRoleId == null) throw new IllegalArgumentException("employeeRoleId is required");
    if (id == null) throw new IllegalArgumentException("role id is required");
    if (name == null || name.isBlank()) throw new IllegalArgumentException("role name is required");
    if (code == null || code.isBlank()) throw new IllegalArgumentException("role code is required");
  }
}
