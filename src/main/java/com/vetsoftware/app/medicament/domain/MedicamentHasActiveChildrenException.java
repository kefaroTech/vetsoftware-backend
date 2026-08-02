package com.vetsoftware.app.medicament.domain;

public class MedicamentHasActiveChildrenException extends RuntimeException {
  public MedicamentHasActiveChildrenException(Long id, String childType) {
    super("Cannot delete medicament " + id + ": has active " + childType + " children");
  }
}
