package com.vetsoftware.app.owner.domain;

public class OwnerHasActiveChildrenException extends RuntimeException {
  public OwnerHasActiveChildrenException(Long id, String childType) {
    super("Cannot delete owner " + id + ": has active " + childType + " children");
  }
}
