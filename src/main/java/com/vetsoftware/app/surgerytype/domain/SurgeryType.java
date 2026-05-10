package com.vetsoftware.app.surgerytype.domain;

import java.time.LocalDateTime;

public class SurgeryType {
    private Long id;
    private String name;
    private String description;
    private final LocalDateTime createdDate;

    public SurgeryType(Long id, String name, String description, LocalDateTime createdDate) {
        validate(name, description);
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdDate = createdDate;
    }

    public static SurgeryType create(String name, String description) {
        return new SurgeryType(null, name, description, LocalDateTime.now());
    }

    public void update(String name, String description) {
        validate(name, description);
        this.name = name;
        this.description = description;
    }

    private static void validate(String name, String description) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description is required");
        if (description.length() > 500) throw new IllegalArgumentException("description must be 500 chars or less");
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
