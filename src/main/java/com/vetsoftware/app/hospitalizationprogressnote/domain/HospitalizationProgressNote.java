package com.vetsoftware.app.hospitalizationprogressnote.domain;

import java.time.LocalDateTime;

public class HospitalizationProgressNote {
    private Long id;
    private String description;
    private HospitalizationRef hospitalization;
    private final EmployeeRef createdBy;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public HospitalizationProgressNote(Long id, String description, HospitalizationRef hospitalization,
                                      EmployeeRef createdBy, LocalDateTime createdDate, boolean enabled) {
        validate(description, hospitalization, createdBy);
        this.id = id;
        this.description = description;
        this.hospitalization = hospitalization;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static HospitalizationProgressNote create(String description, HospitalizationRef hospitalization,
                                                    EmployeeRef createdBy) {
        return new HospitalizationProgressNote(null, description, hospitalization, createdBy,
            LocalDateTime.now(), true);
    }

    public void update(String description) {
        validate(description, this.hospitalization, this.createdBy);
        this.description = description;
    }

    public void enable() { this.enabled = true; }

    public void disable() { this.enabled = false; }

    private static void validate(String description, HospitalizationRef hospitalization, EmployeeRef createdBy) {
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description is required");
        if (description.length() > 2000) throw new IllegalArgumentException("description must be 2000 chars or less");
        if (hospitalization == null) throw new IllegalArgumentException("hospitalization is required");
        if (createdBy == null) throw new IllegalArgumentException("createdBy is required");
    }

    public Long getId() { return id; }
    public String getDescription() { return description; }
    public HospitalizationRef getHospitalization() { return hospitalization; }
    public EmployeeRef getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
}
