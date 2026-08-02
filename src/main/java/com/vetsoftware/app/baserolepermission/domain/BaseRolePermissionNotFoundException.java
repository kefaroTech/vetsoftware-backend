package com.vetsoftware.app.baserolepermission.domain;

public class BaseRolePermissionNotFoundException extends RuntimeException {
  public BaseRolePermissionNotFoundException(Long id) {
    super("BaseRolePermission not found: " + id);
  }
}
