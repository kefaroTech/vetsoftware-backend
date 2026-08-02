package com.vetsoftware.app.hospitalization.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Hospitalization {
    private Long id;
    private LocalDate date;
    private LocalDate startDate;
    private LocalDate endDate;
    private HospitalizationType type;
    private ReasonLeaving reasonLeaving;
    private String reason;
    private String observations;
    private AnimalRef animal;
    private ConsultationRef consultation;
    private CompanyRef company;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public Hospitalization(Long id, LocalDate date, LocalDate startDate, LocalDate endDate,
            HospitalizationType type, ReasonLeaving reasonLeaving, String reason,
            String observations, AnimalRef animal, ConsultationRef consultation, CompanyRef company,
            LocalDateTime createdDate, boolean enabled) {
        validate(date, startDate, endDate, type, reason, observations, animal, company);
        this.id = id;
        this.date = date;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
        this.reasonLeaving = reasonLeaving;
        this.reason = reason;
        this.observations = observations;
        this.animal = animal;
        this.consultation = consultation;
        this.company = company;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static Hospitalization create(LocalDate date, LocalDate startDate, LocalDate endDate,
            HospitalizationType type, ReasonLeaving reasonLeaving, String reason,
            String observations, AnimalRef animal, ConsultationRef consultation,
            CompanyRef company) {
        return new Hospitalization(null, date, startDate, endDate, type, reasonLeaving, reason,
                observations, animal, consultation, company, LocalDateTime.now(), true);
    }

    public void update(LocalDate date, LocalDate startDate, LocalDate endDate,
            HospitalizationType type, ReasonLeaving reasonLeaving, String reason,
            String observations, AnimalRef animal, ConsultationRef consultation,
            CompanyRef company) {
        validate(date, startDate, endDate, type, reason, observations, animal, company);
        this.date = date;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
        this.reasonLeaving = reasonLeaving;
        this.reason = reason;
        this.observations = observations;
        this.animal = animal;
        this.consultation = consultation;
        this.company = company;
    }

    private static void validate(LocalDate date, LocalDate startDate, LocalDate endDate,
            HospitalizationType type, String reason, String observations, AnimalRef animal,
            CompanyRef company) {
        if (date == null)
            throw new IllegalArgumentException("date is required");
        if (startDate == null)
            throw new IllegalArgumentException("startDate is required");
        if (endDate != null && endDate.isBefore(startDate))
            throw new IllegalArgumentException("endDate cannot be before startDate");
        if (type == null)
            throw new IllegalArgumentException("type is required");
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("reason is required");
        if (reason.length() > 500)
            throw new IllegalArgumentException("reason must be 500 chars or less");
        if (observations != null && observations.length() > 2000)
            throw new IllegalArgumentException("observations must be 2000 chars or less");
        if (animal == null)
            throw new IllegalArgumentException("animal is required");
        if (company == null)
            throw new IllegalArgumentException("company is required");
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

    public HospitalizationType getType() {
        return type;
    }

    public ReasonLeaving getReasonLeaving() {
        return reasonLeaving;
    }

    public String getReason() {
        return reason;
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

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }
}
