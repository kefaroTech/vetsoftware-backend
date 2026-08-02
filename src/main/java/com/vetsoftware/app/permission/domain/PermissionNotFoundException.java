package com.vetsoftware.app.permission.domain;

public class PermissionNotFoundException extends RuntimeException {
  public PermissionNotFoundException(Long id) {
    super("Permission not found: " + id);
  }
}
