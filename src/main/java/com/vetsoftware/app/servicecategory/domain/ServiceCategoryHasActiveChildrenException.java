package com.vetsoftware.app.servicecategory.domain;

public class ServiceCategoryHasActiveChildrenException extends RuntimeException {
  public ServiceCategoryHasActiveChildrenException(Long id, String childType) {
    super("Cannot delete servicecategory " + id + ": has active " + childType + " children");
  }
}
