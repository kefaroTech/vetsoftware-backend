package com.vetsoftware.app.prescription.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Prescription {
    private Long id;
    private LocalDate date;
    private String diagnosis;
    private String observations;
    private AnimalRef animal;
    private CompanyRef company;
    private final LocalDateTime createdDate;

    public Prescription(Long id, LocalDate date, String diagnosis, String observations,
                        AnimalRef animal, CompanyRef company, LocalDateTime createdDate) {
        validate(date, diagnosis, observations, animal, company);
        this.id = id;
        this.date = date;
        this.diagnosis = diagnosis;
        this.observations = observations;
        this.animal = animal;
        this.company = company;
        this.createdDate = createdDate;
    }

    public static Prescription create(LocalDate date, String diagnosis, String observations,
                                      AnimalRef animal, CompanyRef company) {
        return new Prescription(null, date, diagnosis, observations, animal, company,
                                LocalDateTime.now());
    }

    public void update(LocalDate date, String diagnosis, String observations,
                       AnimalRef animal, CompanyRef company) {
        validate(date, diagnosis, observations, animal, company);
        this.date = date;
        this.diagnosis = diagnosis;
        this.observations = observations;
        this.animal = animal;
        this.company = company;
    }

    private static void validate(LocalDate date, String diagnosis, String observations,
                                  AnimalRef animal, CompanyRef company) {
        if (date == null) throw new IllegalArgumentException("date is required");
        if (diagnosis == null || diagnosis.isBlank()) throw new IllegalArgumentException("diagnosis is required");
        if (diagnosis.length() > 2000) throw new IllegalArgumentException("diagnosis must be 2000 chars or less");
        if (observations != null && observations.length() > 2000)
            throw new IllegalArgumentException("observations must be 2000 chars or less");
        if (animal == null) throw new IllegalArgumentException("animal is required");
        if (company == null) throw new IllegalArgumentException("company is required");
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public String getDiagnosis() { return diagnosis; }
    public String getObservations() { return observations; }
    public AnimalRef getAnimal() { return animal; }
    public CompanyRef getCompany() { return company; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
