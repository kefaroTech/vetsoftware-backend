package com.vetsoftware.app.servicecategory.domain;

public class ServiceCategoryNotFoundException extends RuntimeException {
  public ServiceCategoryNotFoundException(Long id) {
    super("ServiceCategory not found: " + id);
  }
}
