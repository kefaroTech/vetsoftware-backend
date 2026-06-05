package com.vetsoftware.app.servicecategory.domain;

import java.time.LocalDateTime;

public class ServiceCategory {
    private Long id;
    private String name;
    private String description;
    private CompanyRef company;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public ServiceCategory(Long id, String name, String description,
                           CompanyRef company, LocalDateTime createdDate, boolean enabled) {
        validate(name, description, company);
        this.id = id;
        this.name = name;
        this.description = description;
        this.company = company;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static ServiceCategory create(String name, String description, CompanyRef company) {
        return new ServiceCategory(null, name, description, company, LocalDateTime.now(), true);
    }

    public void update(String name, String description, CompanyRef company) {
        validate(name, description, company);
        this.name = name;
        this.description = description;
        this.company = company;
    }

    private static void validate(String name, String description, CompanyRef company) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description is required");
        if (description.length() > 500) throw new IllegalArgumentException("description must be 500 chars or less");
        if (company == null) throw new IllegalArgumentException("company is required");
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public CompanyRef getCompany() { return company; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
