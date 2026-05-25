package com.vetsoftware.app.deworming.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Deworming {
    private Long id;
    private LocalDate date;
    private LocalDate lastDeworming;
    private DewormingType type;
    private String product;
    private String dosage;
    private LocalDate nextControl;
    private String observations;
    private AnimalRef animal;
    private ConsultationRef consultation;
    private CompanyRef company;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public Deworming(Long id, LocalDate date, LocalDate lastDeworming, DewormingType type,
                     String product, String dosage, LocalDate nextControl, String observations,
                     AnimalRef animal, ConsultationRef consultation, CompanyRef company,
                     LocalDateTime createdDate, boolean enabled) {
        validate(date, type, product, dosage, observations, animal, company);
        this.id = id;
        this.date = date;
        this.lastDeworming = lastDeworming;
        this.type = type;
        this.product = product;
        this.dosage = dosage;
        this.nextControl = nextControl;
        this.observations = observations;
        this.animal = animal;
        this.consultation = consultation;
        this.company = company;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static Deworming create(LocalDate date, LocalDate lastDeworming, DewormingType type,
                                   String product, String dosage, LocalDate nextControl,
                                   String observations, AnimalRef animal,
                                   ConsultationRef consultation, CompanyRef company) {
        return new Deworming(null, date, lastDeworming, type, product, dosage, nextControl,
                             observations, animal, consultation, company, LocalDateTime.now(), true);
    }

    public void update(LocalDate date, LocalDate lastDeworming, DewormingType type,
                       String product, String dosage, LocalDate nextControl,
                       String observations, AnimalRef animal,
                       ConsultationRef consultation, CompanyRef company) {
        validate(date, type, product, dosage, observations, animal, company);
        this.date = date;
        this.lastDeworming = lastDeworming;
        this.type = type;
        this.product = product;
        this.dosage = dosage;
        this.nextControl = nextControl;
        this.observations = observations;
        this.animal = animal;
        this.consultation = consultation;
        this.company = company;
    }

    private static void validate(LocalDate date, DewormingType type, String product, String dosage,
                                  String observations, AnimalRef animal, CompanyRef company) {
        if (date == null) throw new IllegalArgumentException("date is required");
        if (type == null) throw new IllegalArgumentException("type is required");
        if (product == null || product.isBlank()) throw new IllegalArgumentException("product is required");
        if (product.length() > 200) throw new IllegalArgumentException("product must be 200 chars or less");
        if (dosage == null || dosage.isBlank()) throw new IllegalArgumentException("dosage is required");
        if (dosage.length() > 200) throw new IllegalArgumentException("dosage must be 200 chars or less");
        if (observations != null && observations.length() > 2000)
            throw new IllegalArgumentException("observations must be 2000 chars or less");
        if (animal == null) throw new IllegalArgumentException("animal is required");
        if (company == null) throw new IllegalArgumentException("company is required");
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public LocalDate getLastDeworming() { return lastDeworming; }
    public DewormingType getType() { return type; }
    public String getProduct() { return product; }
    public String getDosage() { return dosage; }
    public LocalDate getNextControl() { return nextControl; }
    public String getObservations() { return observations; }
    public AnimalRef getAnimal() { return animal; }
    public ConsultationRef getConsultation() { return consultation; }
    public CompanyRef getCompany() { return company; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
