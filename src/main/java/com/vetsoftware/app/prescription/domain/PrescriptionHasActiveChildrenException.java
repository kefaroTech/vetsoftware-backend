package com.vetsoftware.app.prescription.domain;

public class PrescriptionHasActiveChildrenException extends RuntimeException {
  public PrescriptionHasActiveChildrenException(Long id, String childType) {
    super("Cannot delete prescription " + id + ": has active " + childType + " children");
  }
}
