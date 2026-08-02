package com.vetsoftware.app.systemuser.domain;

public class SystemUserHasActiveChildrenException extends RuntimeException {
  public SystemUserHasActiveChildrenException(Long id, String childType) {
    super("Cannot delete systemuser " + id + ": has active " + childType + " children");
  }
}
