package com.vetsoftware.app.vaccinationtype.domain;

public class VaccinationTypeHasActiveChildrenException extends RuntimeException {
  public VaccinationTypeHasActiveChildrenException(Long id, String childType) {
    super("Cannot delete vaccinationtype " + id + ": has active " + childType + " children");
  }
}
