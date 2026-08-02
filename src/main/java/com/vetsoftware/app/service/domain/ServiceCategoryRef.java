package com.vetsoftware.app.service.domain;

public record ServiceCategoryRef(Long id, String name) {
  public ServiceCategoryRef {
    if (id == null) throw new IllegalArgumentException("serviceCategory id is required");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("serviceCategory name is required");
  }
}
