package com.vetsoftware.app.hospitalizationobservation.domain;

import java.time.LocalDateTime;

public class HospitalizationObservation {
    private Long id;
    private String description;
    private HospitalizationRef hospitalization;
    private final EmployeeRef createdBy;
    private final LocalDateTime createdDate;
    private Long version;
    private boolean enabled;

    public HospitalizationObservation(Long id, String description,
            HospitalizationRef hospitalization, EmployeeRef createdBy, LocalDateTime createdDate,
            Long version, boolean enabled) {
        validate(description, hospitalization, createdBy);
        this.id = id;
        this.description = description;
        this.hospitalization = hospitalization;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.version = version;
        this.enabled = enabled;
    }

    public static HospitalizationObservation create(String description,
            HospitalizationRef hospitalization, EmployeeRef createdBy) {
        return new HospitalizationObservation(null, description, hospitalization, createdBy,
                LocalDateTime.now(), null, true);
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    private static void validate(String description, HospitalizationRef hospitalization,
            EmployeeRef createdBy) {
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("description is required");
        if (description.length() > 2000)
            throw new IllegalArgumentException("description must be 2000 chars or less");
        if (hospitalization == null)
            throw new IllegalArgumentException("hospitalization is required");
        if (createdBy == null)
            throw new IllegalArgumentException("createdBy is required");
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public HospitalizationRef getHospitalization() {
        return hospitalization;
    }

    public EmployeeRef getCreatedBy() {
        return createdBy;
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
}
