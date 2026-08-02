package com.vetsoftware.app.animal.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Serie temporal del peso de un animal. Cada registro es un punto (valor + unidad + fecha) con su
 * origen (manual / consulta / hospitalización). El "peso actual" del animal se deriva del último
 * registro habilitado, por lo que un WeightRecord —no un escalar en Animal— es la fuente de verdad.
 */
public class WeightRecord {
  private Long id;
  private AnimalRef animal;
  private BigDecimal value;
  private WeightType unit;
  private LocalDate measuredAt;
  private WeightSource source;
  private Long sourceId;
  private String note;
  private CompanyRef company;
  private final LocalDateTime createdDate;
  private boolean enabled;

  public WeightRecord(
      Long id,
      AnimalRef animal,
      BigDecimal value,
      WeightType unit,
      LocalDate measuredAt,
      WeightSource source,
      Long sourceId,
      String note,
      CompanyRef company,
      LocalDateTime createdDate,
      boolean enabled) {
    validate(animal, value, unit, measuredAt, source, sourceId, note, company);
    this.id = id;
    this.animal = animal;
    this.value = value;
    this.unit = unit;
    this.measuredAt = measuredAt;
    this.source = source;
    this.sourceId = sourceId;
    this.note = note;
    this.company = company;
    this.createdDate = createdDate;
    this.enabled = enabled;
  }

  public static WeightRecord create(
      AnimalRef animal,
      BigDecimal value,
      WeightType unit,
      LocalDate measuredAt,
      WeightSource source,
      Long sourceId,
      String note,
      CompanyRef company) {
    return new WeightRecord(
        null,
        animal,
        value,
        unit,
        measuredAt,
        source,
        sourceId,
        note,
        company,
        LocalDateTime.now(),
        true);
  }

  private static void validate(
      AnimalRef animal,
      BigDecimal value,
      WeightType unit,
      LocalDate measuredAt,
      WeightSource source,
      Long sourceId,
      String note,
      CompanyRef company) {
    if (animal == null) throw new IllegalArgumentException("animal is required");
    if (value == null) throw new IllegalArgumentException("value is required");
    if (value.signum() <= 0) throw new IllegalArgumentException("value must be greater than zero");
    if (unit == null) throw new IllegalArgumentException("unit is required");
    if (measuredAt == null) throw new IllegalArgumentException("measuredAt is required");
    if (measuredAt.isAfter(LocalDate.now()))
      throw new IllegalArgumentException("measuredAt cannot be in the future");
    if (source == null) throw new IllegalArgumentException("source is required");
    if (source != WeightSource.MANUAL && sourceId == null)
      throw new IllegalArgumentException("sourceId is required when source is " + source);
    if (source == WeightSource.MANUAL && sourceId != null)
      throw new IllegalArgumentException("sourceId must be null for MANUAL records");
    if (note != null && note.length() > 500)
      throw new IllegalArgumentException("note must be 500 chars or less");
    if (company == null) throw new IllegalArgumentException("company is required");
  }

  public Long getId() {
    return id;
  }

  public AnimalRef getAnimal() {
    return animal;
  }

  public BigDecimal getValue() {
    return value;
  }

  public WeightType getUnit() {
    return unit;
  }

  public LocalDate getMeasuredAt() {
    return measuredAt;
  }

  public WeightSource getSource() {
    return source;
  }

  public Long getSourceId() {
    return sourceId;
  }

  public String getNote() {
    return note;
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
