package com.vetsoftware.app.animalalert.domain;

import java.time.LocalDateTime;

public class AnimalAlert {
    private Long id;
    private AnimalRef animal;
    private CompanyRef company;
    private AlertType type;
    private String description;
    private AlertSeverity severity;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public AnimalAlert(Long id, AnimalRef animal, CompanyRef company, AlertType type,
            String description, AlertSeverity severity, LocalDateTime createdDate,
            boolean enabled) {
        validate(type, description, animal, company);
        this.id = id;
        this.animal = animal;
        this.company = company;
        this.type = type;
        this.description = description;
        this.severity = severity;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static AnimalAlert create(AnimalRef animal, AlertType type, String description,
            AlertSeverity severity, CompanyRef company) {
        return new AnimalAlert(null, animal, company, type, description, severity,
                LocalDateTime.now(), true);
    }

    public void update(AlertType type, String description, AlertSeverity severity) {
        validate(type, description, this.animal, this.company);
        this.type = type;
        this.description = description;
        this.severity = severity;
    }

    private static void validate(AlertType type, String description, AnimalRef animal,
            CompanyRef company) {
        if (type == null)
            throw new IllegalArgumentException("type is required");
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("description is required");
        if (description.length() > 255)
            throw new IllegalArgumentException("description must be 255 chars or less");
        if (animal == null)
            throw new IllegalArgumentException("animal is required");
        if (company == null)
            throw new IllegalArgumentException("company is required");
        // severity es opcional
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

    public AlertType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public AlertSeverity getSeverity() {
        return severity;
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
