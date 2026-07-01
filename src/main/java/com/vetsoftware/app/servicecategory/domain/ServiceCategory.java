package com.vetsoftware.app.servicecategory.domain;

import java.time.LocalDateTime;

public class ServiceCategory {
    private Long id;
    private String name;
    private String description;
    private CompanyRef company;
    private final LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private Long updatedBy;
    private boolean enabled;

    public ServiceCategory(Long id, String name, String description,
                           CompanyRef company, LocalDateTime createdDate,
                           LocalDateTime updatedDate, Long updatedBy, boolean enabled) {
        validate(name, description, company);
        this.id = id;
        this.name = name;
        this.description = description;
        this.company = company;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.updatedBy = updatedBy;
        this.enabled = enabled;
    }

    public static ServiceCategory create(String name, String description, CompanyRef company) {
        return new ServiceCategory(null, name, description, company, LocalDateTime.now(), null, null, true);
    }

    public void update(String name, String description, CompanyRef company, Long updatedBy) {
        validate(name, description, company);
        this.name = name;
        this.description = description;
        this.company = company;
        this.updatedDate = LocalDateTime.now();
        this.updatedBy = updatedBy;
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
    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public Long getUpdatedBy() { return updatedBy; }
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
