package com.vetsoftware.app.spa.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Spa {
  private Long id;
  private LocalDate date;
  private SpaTypeRef spaType;
  private String reason;
  private String details;
  private String observations;
  private SpaStatus status;
  private AnimalRef animal;
  private CompanyRef company;
  private final LocalDateTime createdDate;
  private boolean enabled;

  public Spa(
      Long id,
      LocalDate date,
      SpaTypeRef spaType,
      String reason,
      String details,
      String observations,
      SpaStatus status,
      AnimalRef animal,
      CompanyRef company,
      LocalDateTime createdDate,
      boolean enabled) {
    validate(date, spaType, reason, details, observations, status, animal, company);
    this.id = id;
    this.date = date;
    this.spaType = spaType;
    this.reason = reason;
    this.details = details;
    this.observations = observations;
    this.status = status;
    this.animal = animal;
    this.company = company;
    this.createdDate = createdDate;
    this.enabled = enabled;
  }

  public static Spa create(
      LocalDate date,
      SpaTypeRef spaType,
      String reason,
      String details,
      String observations,
      AnimalRef animal,
      CompanyRef company) {
    return new Spa(
        null,
        date,
        spaType,
        reason,
        details,
        observations,
        SpaStatus.AGENDADA,
        animal,
        company,
        LocalDateTime.now(),
        true);
  }

  public void update(
      LocalDate date,
      SpaTypeRef spaType,
      String reason,
      String details,
      String observations,
      AnimalRef animal,
      CompanyRef company) {
    validate(date, spaType, reason, details, observations, this.status, animal, company);
    this.date = date;
    this.spaType = spaType;
    this.reason = reason;
    this.details = details;
    this.observations = observations;
    this.animal = animal;
    this.company = company;
  }

  public void changeStatus(SpaStatus newStatus) {
    if (newStatus == null) throw new IllegalArgumentException("status is required");
    this.status = newStatus;
  }

  public void enable() {
    this.enabled = true;
  }

  public void disable() {
    this.enabled = false;
  }

  private static void validate(
      LocalDate date,
      SpaTypeRef spaType,
      String reason,
      String details,
      String observations,
      SpaStatus status,
      AnimalRef animal,
      CompanyRef company) {
    if (date == null) throw new IllegalArgumentException("date is required");
    if (spaType == null) throw new IllegalArgumentException("spaType is required");
    if (reason == null || reason.isBlank())
      throw new IllegalArgumentException("reason is required");
    if (reason.length() > 2000)
      throw new IllegalArgumentException("reason must be 2000 chars or less");
    if (details == null || details.isBlank())
      throw new IllegalArgumentException("details is required");
    if (details.length() > 2000)
      throw new IllegalArgumentException("details must be 2000 chars or less");
    if (observations == null || observations.isBlank())
      throw new IllegalArgumentException("observations is required");
    if (observations.length() > 2000)
      throw new IllegalArgumentException("observations must be 2000 chars or less");
    if (status == null) throw new IllegalArgumentException("status is required");
    if (animal == null) throw new IllegalArgumentException("animal is required");
    if (company == null) throw new IllegalArgumentException("company is required");
  }

  public Long getId() {
    return id;
  }

  public LocalDate getDate() {
    return date;
  }

  public SpaTypeRef getSpaType() {
    return spaType;
  }

  public String getReason() {
    return reason;
  }

  public String getDetails() {
    return details;
  }

  public String getObservations() {
    return observations;
  }

  public SpaStatus getStatus() {
    return status;
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
