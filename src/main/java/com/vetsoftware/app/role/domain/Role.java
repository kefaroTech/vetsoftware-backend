package com.vetsoftware.app.role.domain;

import java.time.LocalDateTime;

public class Role {
  private Long id;
  private String name;
  private String code;
  private CompanyRef company;
  private final LocalDateTime createdDate;
  private boolean enabled;

  public Role(
      Long id,
      String name,
      String code,
      CompanyRef company,
      LocalDateTime createdDate,
      boolean enabled) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
    if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
    if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
    if (code.length() > 50) throw new IllegalArgumentException("code must be 50 chars or less");
    if (company == null) throw new IllegalArgumentException("company is required");
    this.id = id;
    this.name = name;
    this.code = code;
    this.company = company;
    this.createdDate = createdDate;
    this.enabled = enabled;
  }

  public static Role create(String name, String code, CompanyRef company) {
    return new Role(null, name, code, company, LocalDateTime.now(), true);
  }

  public void update(String name, String code, CompanyRef company) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
    if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
    if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
    if (code.length() > 50) throw new IllegalArgumentException("code must be 50 chars or less");
    if (company == null) throw new IllegalArgumentException("company is required");
    this.name = name;
    this.code = code;
    this.company = company;
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

  public CompanyRef getCompany() {
    return company;
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
