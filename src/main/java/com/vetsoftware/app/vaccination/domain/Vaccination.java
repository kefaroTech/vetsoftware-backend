package com.vetsoftware.app.vaccination.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Vaccination {
    private Long id;
    private LocalDate date;
    private VaccinationTypeRef vaccinationType;
    private String lot;
    private String notes;
    private LocalDate nextVaccination;
    private AnimalRef animal;
    private CompanyRef company;
    private final LocalDateTime createdDate;

    public Vaccination(Long id, LocalDate date, VaccinationTypeRef vaccinationType,
                       String lot, String notes, LocalDate nextVaccination,
                       AnimalRef animal, CompanyRef company, LocalDateTime createdDate) {
        validate(date, vaccinationType, lot, notes, animal, company);
        this.id = id;
        this.date = date;
        this.vaccinationType = vaccinationType;
        this.lot = lot;
        this.notes = notes;
        this.nextVaccination = nextVaccination;
        this.animal = animal;
        this.company = company;
        this.createdDate = createdDate;
    }

    public static Vaccination create(LocalDate date, VaccinationTypeRef vaccinationType,
                                     String lot, String notes, LocalDate nextVaccination,
                                     AnimalRef animal, CompanyRef company) {
        return new Vaccination(null, date, vaccinationType, lot, notes, nextVaccination,
                               animal, company, LocalDateTime.now());
    }

    public void update(LocalDate date, VaccinationTypeRef vaccinationType,
                       String lot, String notes, LocalDate nextVaccination,
                       AnimalRef animal, CompanyRef company) {
        validate(date, vaccinationType, lot, notes, animal, company);
        this.date = date;
        this.vaccinationType = vaccinationType;
        this.lot = lot;
        this.notes = notes;
        this.nextVaccination = nextVaccination;
        this.animal = animal;
        this.company = company;
    }

    private static void validate(LocalDate date, VaccinationTypeRef vaccinationType,
                                  String lot, String notes, AnimalRef animal, CompanyRef company) {
        if (date == null) throw new IllegalArgumentException("date is required");
        if (vaccinationType == null) throw new IllegalArgumentException("vaccinationType is required");
        if (lot == null || lot.isBlank()) throw new IllegalArgumentException("lot is required");
        if (lot.length() > 100) throw new IllegalArgumentException("lot must be 100 chars or less");
        if (notes != null && notes.length() > 2000) throw new IllegalArgumentException("notes must be 2000 chars or less");
        if (animal == null) throw new IllegalArgumentException("animal is required");
        if (company == null) throw new IllegalArgumentException("company is required");
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public VaccinationTypeRef getVaccinationType() { return vaccinationType; }
    public String getLot() { return lot; }
    public String getNotes() { return notes; }
    public LocalDate getNextVaccination() { return nextVaccination; }
    public AnimalRef getAnimal() { return animal; }
    public CompanyRef getCompany() { return company; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
