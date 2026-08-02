package com.vetsoftware.app.submodule.domain;

import java.time.LocalDateTime;

public class SubModule {
  private Long id;
  private String name;
  private String code;
  private ModuleRef module;
  private final LocalDateTime createdDate;
  private boolean enabled;

  public SubModule(
      Long id,
      String name,
      String code,
      ModuleRef module,
      LocalDateTime createdDate,
      boolean enabled) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
    if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
    if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
    if (code.length() > 50) throw new IllegalArgumentException("code must be 50 chars or less");
    if (module == null) throw new IllegalArgumentException("module is required");
    this.id = id;
    this.name = name;
    this.code = code;
    this.module = module;
    this.createdDate = createdDate;
    this.enabled = enabled;
  }

  public static SubModule create(String name, String code, ModuleRef module) {
    return new SubModule(null, name, code, module, LocalDateTime.now(), true);
  }

  public void update(String name, String code, ModuleRef module) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
    if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
    if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
    if (code.length() > 50) throw new IllegalArgumentException("code must be 50 chars or less");
    if (module == null) throw new IllegalArgumentException("module is required");
    this.name = name;
    this.code = code;
    this.module = module;
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

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getCode() {
    return code;
  }

  public ModuleRef getModule() {
    return module;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }
}
