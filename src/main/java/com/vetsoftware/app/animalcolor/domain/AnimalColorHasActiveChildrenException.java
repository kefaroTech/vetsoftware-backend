package com.vetsoftware.app.animalcolor.domain;

public class AnimalColorHasActiveChildrenException extends RuntimeException {
  public AnimalColorHasActiveChildrenException(Long id, String childType) {
    super("Cannot delete animalcolor " + id + ": has active " + childType + " children");
  }
}
