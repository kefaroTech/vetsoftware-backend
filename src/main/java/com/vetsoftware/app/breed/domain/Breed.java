package com.vetsoftware.app.breed.domain;

import java.time.LocalDateTime;

public class Breed {
  private Long id;
  private String name;
  private SpecieRef specie;
  private final LocalDateTime createdDate;
  private boolean enabled;

  public Breed(Long id, String name, SpecieRef specie, LocalDateTime createdDate, boolean enabled) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
    if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
    if (specie == null) throw new IllegalArgumentException("specie is required");
    this.id = id;
    this.name = name;
    this.specie = specie;
    this.createdDate = createdDate;
    this.enabled = enabled;
  }

  public static Breed create(String name, SpecieRef specie) {
    return new Breed(null, name, specie, LocalDateTime.now(), true);
  }

  public void update(String name, SpecieRef specie) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
    if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
    if (specie == null) throw new IllegalArgumentException("specie is required");
    this.name = name;
    this.specie = specie;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public SpecieRef getSpecie() {
    return specie;
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
