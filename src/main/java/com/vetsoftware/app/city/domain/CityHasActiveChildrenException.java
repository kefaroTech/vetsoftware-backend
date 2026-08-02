package com.vetsoftware.app.city.domain;

public class CityHasActiveChildrenException extends RuntimeException {
  public CityHasActiveChildrenException(Long id, String childType) {
    super("Cannot delete city " + id + ": has active " + childType + " children");
  }
}
