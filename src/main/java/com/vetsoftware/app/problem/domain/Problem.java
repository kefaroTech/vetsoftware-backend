package com.vetsoftware.app.problem.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Problem {
    private Long id;
    private AnimalRef animal;
    private CompanyRef company;
    private String description;
    private ProblemStatus status;
    private LocalDate onsetDate;
    private LocalDate resolvedDate;
    private String notes;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public Problem(Long id, AnimalRef animal, CompanyRef company, String description,
            ProblemStatus status, LocalDate onsetDate, LocalDate resolvedDate, String notes,
            LocalDateTime createdDate, boolean enabled) {
        validate(description, status, notes, animal, company);
        this.id = id;
        this.animal = animal;
        this.company = company;
        this.description = description;
        this.status = status;
        this.onsetDate = onsetDate;
        this.resolvedDate = resolvedDate;
        this.notes = blankToNull(notes);
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static Problem create(AnimalRef animal, CompanyRef company, String description,
            ProblemStatus status, LocalDate onsetDate, LocalDate resolvedDate, String notes) {
        return new Problem(null, animal, company, description, status, onsetDate, resolvedDate,
                notes, LocalDateTime.now(), true);
    }

    public void update(String description, ProblemStatus status, LocalDate onsetDate,
            LocalDate resolvedDate, String notes, CompanyRef company) {
        validate(description, status, notes, this.animal, company);
        this.description = description;
        this.status = status;
        this.onsetDate = onsetDate;
        this.resolvedDate = resolvedDate;
        this.notes = blankToNull(notes);
        this.company = company;
    }

    private static void validate(String description, ProblemStatus status, String notes,
            AnimalRef animal, CompanyRef company) {
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("description is required");
        if (description.length() > 255)
            throw new IllegalArgumentException("description must be 255 chars or less");
        if (status == null)
            throw new IllegalArgumentException("status is required");
        if (notes != null && notes.length() > 2000)
            throw new IllegalArgumentException("notes must be 2000 chars or less");
        if (animal == null)
            throw new IllegalArgumentException("animal is required");
        if (company == null)
            throw new IllegalArgumentException("company is required");
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    public Long getId() {
        return id;
    }

    public AnimalRef getAnimal() {
        return animal;
    }

    public CompanyRef getCompany() {
        return company;
    }

    public String getDescription() {
        return description;
    }

    public ProblemStatus getStatus() {
        return status;
    }

    public LocalDate getOnsetDate() {
        return onsetDate;
    }

    public LocalDate getResolvedDate() {
        return resolvedDate;
    }

    public String getNotes() {
        return notes;
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
