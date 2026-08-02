package com.vetsoftware.app.permission.domain;

public class PermissionHasActiveChildrenException extends RuntimeException {
  public PermissionHasActiveChildrenException(Long id, String childType) {
    super("Cannot delete permission " + id + ": has active " + childType + " children");
  }
}
