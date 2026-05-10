package com.vetsoftware.app.surgery.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Surgery {
    private Long id;
    private LocalDate date;
    private SurgeryTypeRef surgeryType;
    private String description;
    private String medicament;
    private String observations;
    private String complications;
    private AnimalRef animal;
    private ConsultationRef consultation;
    private CompanyRef company;
    private final LocalDateTime createdDate;

    public Surgery(Long id, LocalDate date, SurgeryTypeRef surgeryType, String description,
                   String medicament, String observations, String complications,
                   AnimalRef animal, ConsultationRef consultation, CompanyRef company,
                   LocalDateTime createdDate) {
        validate(date, surgeryType, description, medicament, observations, complications, animal, company);
        this.id = id;
        this.date = date;
        this.surgeryType = surgeryType;
        this.description = description;
        this.medicament = medicament;
        this.observations = observations;
        this.complications = complications;
        this.animal = animal;
        this.consultation = consultation;
        this.company = company;
        this.createdDate = createdDate;
    }

    public static Surgery create(LocalDate date, SurgeryTypeRef surgeryType, String description,
                                 String medicament, String observations, String complications,
                                 AnimalRef animal, ConsultationRef consultation, CompanyRef company) {
        return new Surgery(null, date, surgeryType, description, medicament, observations,
                           complications, animal, consultation, company, LocalDateTime.now());
    }

    public void update(LocalDate date, SurgeryTypeRef surgeryType, String description,
                       String medicament, String observations, String complications,
                       AnimalRef animal, ConsultationRef consultation, CompanyRef company) {
        validate(date, surgeryType, description, medicament, observations, complications, animal, company);
        this.date = date;
        this.surgeryType = surgeryType;
        this.description = description;
        this.medicament = medicament;
        this.observations = observations;
        this.complications = complications;
        this.animal = animal;
        this.consultation = consultation;
        this.company = company;
    }

    private static void validate(LocalDate date, SurgeryTypeRef surgeryType, String description,
                                  String medicament, String observations, String complications,
                                  AnimalRef animal, CompanyRef company) {
        if (date == null) throw new IllegalArgumentException("date is required");
        if (surgeryType == null) throw new IllegalArgumentException("surgeryType is required");
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description is required");
        if (description.length() > 2000) throw new IllegalArgumentException("description must be 2000 chars or less");
        if (medicament != null && medicament.length() > 200)
            throw new IllegalArgumentException("medicament must be 200 chars or less");
        if (observations != null && observations.length() > 2000)
            throw new IllegalArgumentException("observations must be 2000 chars or less");
        if (complications != null && complications.length() > 2000)
            throw new IllegalArgumentException("complications must be 2000 chars or less");
        if (animal == null) throw new IllegalArgumentException("animal is required");
        if (company == null) throw new IllegalArgumentException("company is required");
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public SurgeryTypeRef getSurgeryType() { return surgeryType; }
    public String getDescription() { return description; }
    public String getMedicament() { return medicament; }
    public String getObservations() { return observations; }
    public String getComplications() { return complications; }
    public AnimalRef getAnimal() { return animal; }
    public ConsultationRef getConsultation() { return consultation; }
    public CompanyRef getCompany() { return company; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
