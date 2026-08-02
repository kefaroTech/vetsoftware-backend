package com.vetsoftware.app.basepermission.domain;

import java.time.LocalDateTime;

public class BasePermission {
  private Long id;
  private String name;
  private String code;
  private SubModuleRef subModule;
  private final LocalDateTime createdDate;
  private boolean enabled;

  public BasePermission(
      Long id,
      String name,
      String code,
      SubModuleRef subModule,
      LocalDateTime createdDate,
      boolean enabled) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
    if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
    if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
    if (code.length() > 50) throw new IllegalArgumentException("code must be 50 chars or less");
    if (subModule == null) throw new IllegalArgumentException("subModule is required");
    this.id = id;
    this.name = name;
    this.code = code;
    this.subModule = subModule;
    this.createdDate = createdDate;
    this.enabled = enabled;
  }

  public static BasePermission create(String name, String code, SubModuleRef subModule) {
    return new BasePermission(null, name, code, subModule, LocalDateTime.now(), true);
  }

  public void update(String name, String code, SubModuleRef subModule) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
    if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
    if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
    if (code.length() > 50) throw new IllegalArgumentException("code must be 50 chars or less");
    if (subModule == null) throw new IllegalArgumentException("subModule is required");
    this.name = name;
    this.code = code;
    this.subModule = subModule;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getCode() {
    return code;
  }

  public SubModuleRef getSubModule() {
    return subModule;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void enable() {
    this.enabled = true;
  }

  public void disable() {
    this.enabled = false;
  }
}
