package com.vetsoftware.app.daycare.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DayCare {
  private Long id;
  private LocalDate date;
  private LocalDate startDate;
  private LocalDate endDate;
  private DayCareType type;
  private String objects;
  private String observations;
  private AnimalRef animal;
  private CompanyRef company;
  private final LocalDateTime createdDate;
  private boolean enabled;

  public DayCare(
      Long id,
      LocalDate date,
      LocalDate startDate,
      LocalDate endDate,
      DayCareType type,
      String objects,
      String observations,
      AnimalRef animal,
      CompanyRef company,
      LocalDateTime createdDate,
      boolean enabled) {
    validate(date, startDate, endDate, type, objects, observations, animal, company);
    this.id = id;
    this.date = date;
    this.startDate = startDate;
    this.endDate = endDate;
    this.type = type;
    this.objects = objects;
    this.observations = observations;
    this.animal = animal;
    this.company = company;
    this.createdDate = createdDate;
    this.enabled = enabled;
  }

  public static DayCare create(
      LocalDate date,
      LocalDate startDate,
      LocalDate endDate,
      DayCareType type,
      String objects,
      String observations,
      AnimalRef animal,
      CompanyRef company) {
    return new DayCare(
        null,
        date,
        startDate,
        endDate,
        type,
        objects,
        observations,
        animal,
        company,
        LocalDateTime.now(),
        true);
  }

  public void update(
      LocalDate date,
      LocalDate startDate,
      LocalDate endDate,
      DayCareType type,
      String objects,
      String observations,
      AnimalRef animal,
      CompanyRef company) {
    validate(date, startDate, endDate, type, objects, observations, animal, company);
    this.date = date;
    this.startDate = startDate;
    this.endDate = endDate;
    this.type = type;
    this.objects = objects;
    this.observations = observations;
    this.animal = animal;
    this.company = company;
  }

  public void enable() {
    this.enabled = true;
  }

  public void disable() {
    this.enabled = false;
  }

  private static void validate(
      LocalDate date,
      LocalDate startDate,
      LocalDate endDate,
      DayCareType type,
      String objects,
      String observations,
      AnimalRef animal,
      CompanyRef company) {
    if (date == null) throw new IllegalArgumentException("date is required");
    if (startDate == null) throw new IllegalArgumentException("startDate is required");
    if (endDate != null && endDate.isBefore(startDate))
      throw new IllegalArgumentException("endDate cannot be before startDate");
    if (type == null) throw new IllegalArgumentException("type is required");
    if (objects != null && objects.length() > 1000)
      throw new IllegalArgumentException("objects must be 1000 chars or less");
    if (observations != null && observations.length() > 2000)
      throw new IllegalArgumentException("observations must be 2000 chars or less");
    if (animal == null) throw new IllegalArgumentException("animal is required");
    if (company == null) throw new IllegalArgumentException("company is required");
  }

  public Long getId() {
    return id;
  }

  public LocalDate getDate() {
    return date;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public DayCareType getType() {
    return type;
  }

  public String getObjects() {
    return objects;
  }

  public String getObservations() {
    return observations;
  }

  public AnimalRef getAnimal() {
    return animal;
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
}
