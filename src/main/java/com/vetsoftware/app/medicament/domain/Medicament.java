package com.vetsoftware.app.medicament.domain;

import java.time.LocalDateTime;

public class Medicament {
    private Long id;
    private String name;
    private String description;
    private CompanyRef company;
    private boolean general;
    private final LocalDateTime createdDate;
    private Long version;
    private boolean enabled;

    public Medicament(Long id, String name, String description, CompanyRef company, boolean general,
            LocalDateTime createdDate, Long version, boolean enabled) {
        validate(name, description, company, general);
        this.id = id;
        this.name = name;
        this.description = description;
        this.company = company;
        this.general = general;
        this.createdDate = createdDate;
        this.version = version;
        this.enabled = enabled;
    }

    public static Medicament create(String name, String description, CompanyRef company,
            boolean general) {
        return new Medicament(null, name, description, company, general, LocalDateTime.now(), null,
                true);
    }

    public void update(String name, String description, CompanyRef company, boolean general) {
        validate(name, description, company, general);
        this.name = name;
        this.description = description;
        this.company = company;
        this.general = general;
    }

    private static void validate(String name, String description, CompanyRef company,
            boolean general) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name is required");
        if (name.length() > 200)
            throw new IllegalArgumentException("name must be 200 chars or less");
        if (description != null && description.length() > 500)
            throw new IllegalArgumentException("description must be 500 chars or less");
        if (general && company != null)
            throw new IllegalArgumentException("general medicament cannot have company");
        if (!general && company == null)
            throw new IllegalArgumentException("non-general medicament requires company");
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public CompanyRef getCompany() {
        return company;
    }

    public boolean isGeneral() {
        return general;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
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
