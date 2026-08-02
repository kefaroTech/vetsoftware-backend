package com.vetsoftware.app.prescription.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Prescription {
  private Long id;
  private LocalDate date;
  private String diagnosis;
  private String observations;
  private AnimalRef animal;
  private ConsultationRef consultation;
  private CompanyRef company;
  private final LocalDateTime createdDate;
  private boolean enabled;

  public Prescription(
      Long id,
      LocalDate date,
      String diagnosis,
      String observations,
      AnimalRef animal,
      ConsultationRef consultation,
      CompanyRef company,
      LocalDateTime createdDate,
      boolean enabled) {
    validate(date, diagnosis, observations, animal, consultation, company);
    this.id = id;
    this.date = date;
    this.diagnosis = blankToNull(diagnosis);
    this.observations = observations;
    this.animal = animal;
    this.consultation = consultation;
    this.company = company;
    this.createdDate = createdDate;
    this.enabled = enabled;
  }

  public static Prescription create(
      LocalDate date,
      String diagnosis,
      String observations,
      AnimalRef animal,
      ConsultationRef consultation,
      CompanyRef company) {
    return new Prescription(
        null,
        date,
        diagnosis,
        observations,
        animal,
        consultation,
        company,
        LocalDateTime.now(),
        true);
  }

  public void update(
      LocalDate date,
      String diagnosis,
      String observations,
      AnimalRef animal,
      ConsultationRef consultation,
      CompanyRef company) {
    validate(date, diagnosis, observations, animal, consultation, company);
    this.date = date;
    this.diagnosis = blankToNull(diagnosis);
    this.observations = observations;
    this.animal = animal;
    this.consultation = consultation;
    this.company = company;
  }

  public void enable() {
    this.enabled = true;
  }

  public void disable() {
    this.enabled = false;
  }

  private static String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }

  private static void validate(
      LocalDate date,
      String diagnosis,
      String observations,
      AnimalRef animal,
      ConsultationRef consultation,
      CompanyRef company) {
    if (date == null) throw new IllegalArgumentException("date is required");
    // El diagnóstico proviene de la consulta y es opcional (la consulta puede no tenerlo aún);
    // solo se valida el tope de longitud cuando viene con contenido.
    if (diagnosis != null && diagnosis.length() > 2000)
      throw new IllegalArgumentException("diagnosis must be 2000 chars or less");
    if (observations != null && observations.length() > 2000)
      throw new IllegalArgumentException("observations must be 2000 chars or less");
    if (animal == null) throw new IllegalArgumentException("animal is required");
    if (consultation == null) throw new IllegalArgumentException("consultation is required");
    if (company == null) throw new IllegalArgumentException("company is required");
  }

  public Long getId() {
    return id;
  }

  public LocalDate getDate() {
    return date;
  }

  public String getDiagnosis() {
    return diagnosis;
  }

  public String getObservations() {
    return observations;
  }

  public AnimalRef getAnimal() {
    return animal;
  }

  public ConsultationRef getConsultation() {
    return consultation;
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
