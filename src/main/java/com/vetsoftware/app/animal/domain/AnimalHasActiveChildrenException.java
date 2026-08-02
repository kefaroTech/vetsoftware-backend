package com.vetsoftware.app.animal.domain;

public class AnimalHasActiveChildrenException extends RuntimeException {
  public AnimalHasActiveChildrenException(Long id, String childType) {
    super("Cannot delete animal " + id + ": has active " + childType + " children");
  }
}
