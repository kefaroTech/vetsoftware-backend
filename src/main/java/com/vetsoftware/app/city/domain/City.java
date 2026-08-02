package com.vetsoftware.app.city.domain;

import java.time.LocalDateTime;

public class City {
  private Long id;
  private String name;
  private StateRef state;
  private String daneCode;
  private final LocalDateTime createdDate;
  private boolean enabled;

  public City(
      Long id,
      String name,
      StateRef state,
      String daneCode,
      LocalDateTime createdDate,
      boolean enabled) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
    if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
    if (state == null) throw new IllegalArgumentException("state is required");
    this.id = id;
    this.name = name;
    this.state = state;
    this.daneCode = daneCode;
    this.createdDate = createdDate;
    this.enabled = enabled;
  }

  public static City create(String name, StateRef state, String daneCode) {
    return new City(null, name, state, daneCode, LocalDateTime.now(), true);
  }

  public void update(String name, StateRef state, String daneCode) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
    if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
    if (state == null) throw new IllegalArgumentException("state is required");
    this.name = name;
    this.state = state;
    this.daneCode = daneCode;
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

  public StateRef getState() {
    return state;
  }

  public String getDaneCode() {
    return daneCode;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }
}
