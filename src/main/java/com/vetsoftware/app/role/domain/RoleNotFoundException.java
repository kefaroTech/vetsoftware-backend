package com.vetsoftware.app.role.domain;

public class RoleNotFoundException extends RuntimeException {
  public RoleNotFoundException(Long id) {
    super("Role not found: " + id);
  }
}
