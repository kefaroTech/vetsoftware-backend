package com.vetsoftware.app.animal.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Animal {
  private Long id;
  private String name;
  private String code;
  private SpecieRef specie;
  private BreedRef breed;
  private OwnerRef owner;
  private Gender gender;
  private WeightType weightType;
  private AnimalType animalType;
  private ReproductiveState reproductiveState;
  private AnimalColorRef color;
  private LocalDate bod;
  private Integer size;
  private boolean deceased;
  private LocalDate deceasedDate;
  private CompanyRef company;
  private final LocalDateTime createdDate;
  private boolean enabled;

  // Peso actual DERIVADO del último WeightRecord habilitado (no es una columna). Lo hidrata el
  // adaptador de persistencia vía applyCurrentWeight(); create/update no lo tocan. Ver
  // WeightRecord.
  private BigDecimal currentWeight;
  private WeightType currentWeightUnit;
  private LocalDate currentWeightMeasuredAt;

  public Animal(
      Long id,
      String name,
      String code,
      SpecieRef specie,
      BreedRef breed,
      OwnerRef owner,
      Gender gender,
      WeightType weightType,
      AnimalType animalType,
      ReproductiveState reproductiveState,
      AnimalColorRef color,
      LocalDate bod,
      Integer size,
      boolean deceased,
      LocalDate deceasedDate,
      CompanyRef company,
      LocalDateTime createdDate,
      boolean enabled) {
    validate(
        name,
        code,
        specie,
        breed,
        owner,
        gender,
        weightType,
        animalType,
        reproductiveState,
        color,
        size,
        deceased,
        deceasedDate,
        company);
    this.id = id;
    this.name = name;
    this.code = code;
    this.specie = specie;
    this.breed = breed;
    this.owner = owner;
    this.gender = gender;
    this.weightType = weightType;
    this.animalType = animalType;
    this.reproductiveState = reproductiveState;
    this.color = color;
    this.bod = bod;
    this.size = size;
    this.deceased = deceased;
    this.deceasedDate = deceasedDate;
    this.company = company;
    this.createdDate = createdDate;
    this.enabled = enabled;
  }

  public static Animal create(
      String name,
      String code,
      SpecieRef specie,
      BreedRef breed,
      OwnerRef owner,
      Gender gender,
      WeightType weightType,
      AnimalType animalType,
      ReproductiveState reproductiveState,
      AnimalColorRef color,
      LocalDate bod,
      Integer size,
      boolean deceased,
      LocalDate deceasedDate,
      CompanyRef company) {
    return new Animal(
        null,
        name,
        code,
        specie,
        breed,
        owner,
        gender,
        weightType,
        animalType,
        reproductiveState,
        color,
        bod,
        size,
        deceased,
        deceasedDate,
        company,
        LocalDateTime.now(),
        true);
  }

  public void update(
      String name,
      String code,
      SpecieRef specie,
      BreedRef breed,
      OwnerRef owner,
      Gender gender,
      WeightType weightType,
      AnimalType animalType,
      ReproductiveState reproductiveState,
      AnimalColorRef color,
      LocalDate bod,
      Integer size,
      boolean deceased,
      LocalDate deceasedDate,
      CompanyRef company) {
    validate(
        name,
        code,
        specie,
        breed,
        owner,
        gender,
        weightType,
        animalType,
        reproductiveState,
        color,
        size,
        deceased,
        deceasedDate,
        company);
    this.name = name;
    this.code = code;
    this.specie = specie;
    this.breed = breed;
    this.owner = owner;
    this.gender = gender;
    this.weightType = weightType;
    this.animalType = animalType;
    this.reproductiveState = reproductiveState;
    this.color = color;
    this.bod = bod;
    this.size = size;
    this.deceased = deceased;
    this.deceasedDate = deceasedDate;
    this.company = company;
  }

  /**
   * Hidrata el peso actual derivado del último registro de peso. Solo lo llama la capa de
   * persistencia.
   */
  public void applyCurrentWeight(BigDecimal value, WeightType unit, LocalDate measuredAt) {
    this.currentWeight = value;
    this.currentWeightUnit = unit;
    this.currentWeightMeasuredAt = measuredAt;
  }

  private static void validate(
      String name,
      String code,
      SpecieRef specie,
      BreedRef breed,
      OwnerRef owner,
      Gender gender,
      WeightType weightType,
      AnimalType animalType,
      ReproductiveState reproductiveState,
      AnimalColorRef color,
      Integer size,
      boolean deceased,
      LocalDate deceasedDate,
      CompanyRef company) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
    if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
    if (code != null && code.length() > 50)
      throw new IllegalArgumentException("code must be 50 chars or less");
    if (specie == null) throw new IllegalArgumentException("specie is required");
    if (breed == null) throw new IllegalArgumentException("breed is required");
    if (owner == null) throw new IllegalArgumentException("owner is required");
    if (gender == null) throw new IllegalArgumentException("gender is required");
    if (weightType == null) throw new IllegalArgumentException("weightType is required");
    if (animalType == null) throw new IllegalArgumentException("animalType is required");
    if (reproductiveState == null)
      throw new IllegalArgumentException("reproductiveState is required");
    if (color == null) throw new IllegalArgumentException("color is required");
    if (size != null && size < 0) throw new IllegalArgumentException("size cannot be negative");
    if (company == null) throw new IllegalArgumentException("company is required");
    if (deceased && deceasedDate == null)
      throw new IllegalArgumentException("deceasedDate is required when deceased is true");
    if (!deceased && deceasedDate != null)
      throw new IllegalArgumentException("deceasedDate must be null when deceased is false");
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

  public SpecieRef getSpecie() {
    return specie;
  }

  public BreedRef getBreed() {
    return breed;
  }

  public OwnerRef getOwner() {
    return owner;
  }

  public Gender getGender() {
    return gender;
  }

  public WeightType getWeightType() {
    return weightType;
  }

  public AnimalType getAnimalType() {
    return animalType;
  }

  public ReproductiveState getReproductiveState() {
    return reproductiveState;
  }

  public AnimalColorRef getColor() {
    return color;
  }

  public LocalDate getBod() {
    return bod;
  }

  public Integer getSize() {
    return size;
  }

  public boolean isDeceased() {
    return deceased;
  }

  public LocalDate getDeceasedDate() {
    return deceasedDate;
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

  public BigDecimal getCurrentWeight() {
    return currentWeight;
  }

  public WeightType getCurrentWeightUnit() {
    return currentWeightUnit;
  }

  public LocalDate getCurrentWeightMeasuredAt() {
    return currentWeightMeasuredAt;
  }
}
