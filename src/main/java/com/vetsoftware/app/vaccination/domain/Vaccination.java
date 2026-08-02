package com.vetsoftware.app.vaccination.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Vaccination {
    private Long id;
    private LocalDate date;
    private VaccinationTypeRef vaccinationType;
    private String lot;
    private String notes;
    private String route;
    private String applicationSite;
    private LocalDate nextVaccination;
    private AnimalRef animal;
    private ConsultationRef consultation;
    private CompanyRef company;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public Vaccination(Long id, LocalDate date, VaccinationTypeRef vaccinationType, String lot,
            String notes, String route, String applicationSite, LocalDate nextVaccination,
            AnimalRef animal, ConsultationRef consultation, CompanyRef company,
            LocalDateTime createdDate, boolean enabled) {
        validate(date, vaccinationType, lot, notes, route, applicationSite, animal, company);
        this.id = id;
        this.date = date;
        this.vaccinationType = vaccinationType;
        this.lot = lot;
        this.notes = notes;
        this.route = blankToNull(route);
        this.applicationSite = blankToNull(applicationSite);
        this.nextVaccination = nextVaccination;
        this.animal = animal;
        this.consultation = consultation;
        this.company = company;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static Vaccination create(LocalDate date, VaccinationTypeRef vaccinationType, String lot,
            String notes, String route, String applicationSite, LocalDate nextVaccination,
            AnimalRef animal, ConsultationRef consultation, CompanyRef company) {
        return new Vaccination(null, date, vaccinationType, lot, notes, route, applicationSite,
                nextVaccination, animal, consultation, company, LocalDateTime.now(), true);
    }

    public void update(LocalDate date, VaccinationTypeRef vaccinationType, String lot, String notes,
            String route, String applicationSite, LocalDate nextVaccination, AnimalRef animal,
            ConsultationRef consultation, CompanyRef company) {
        validate(date, vaccinationType, lot, notes, route, applicationSite, animal, company);
        this.date = date;
        this.vaccinationType = vaccinationType;
        this.lot = lot;
        this.notes = notes;
        this.route = blankToNull(route);
        this.applicationSite = blankToNull(applicationSite);
        this.nextVaccination = nextVaccination;
        this.animal = animal;
        this.consultation = consultation;
        this.company = company;
    }

    private static void validate(LocalDate date, VaccinationTypeRef vaccinationType, String lot,
            String notes, String route, String applicationSite, AnimalRef animal,
            CompanyRef company) {
        if (date == null)
            throw new IllegalArgumentException("date is required");
        if (vaccinationType == null)
            throw new IllegalArgumentException("vaccinationType is required");
        if (lot == null || lot.isBlank())
            throw new IllegalArgumentException("lot is required");
        if (lot.length() > 100)
            throw new IllegalArgumentException("lot must be 100 chars or less");
        if (notes != null && notes.length() > 2000)
            throw new IllegalArgumentException("notes must be 2000 chars or less");
        // route (vía) y applicationSite (sitio) son opcionales (WSAVA); solo se valida
        // la longitud.
        if (route != null && route.length() > 30)
            throw new IllegalArgumentException("route must be 30 chars or less");
        if (applicationSite != null && applicationSite.length() > 60)
            throw new IllegalArgumentException("applicationSite must be 60 chars or less");
        if (animal == null)
            throw new IllegalArgumentException("animal is required");
        if (company == null)
            throw new IllegalArgumentException("company is required");
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.strip();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public VaccinationTypeRef getVaccinationType() {
        return vaccinationType;
    }

    public String getLot() {
        return lot;
    }

    public String getNotes() {
        return notes;
    }

    public String getRoute() {
        return route;
    }

    public String getApplicationSite() {
        return applicationSite;
    }

    public LocalDate getNextVaccination() {
        return nextVaccination;
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

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }
}
