package com.vetsoftware.app.breed.domain;

public class BreedHasActiveChildrenException extends RuntimeException {
  public BreedHasActiveChildrenException(Long id, String childType) {
    super("Cannot delete breed " + id + ": has active " + childType + " children");
  }
}
