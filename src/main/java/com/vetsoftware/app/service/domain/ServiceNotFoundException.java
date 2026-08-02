package com.vetsoftware.app.service.domain;

public class ServiceNotFoundException extends RuntimeException {
  public ServiceNotFoundException(Long id) {
    super("Service not found: " + id);
  }
}
