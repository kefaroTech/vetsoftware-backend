package com.vetsoftware.app.role.domain;

public class RoleHasActiveChildrenException extends RuntimeException {
  public RoleHasActiveChildrenException(Long id, String childType) {
    super("Cannot delete role " + id + ": has active " + childType + " children");
  }
}
