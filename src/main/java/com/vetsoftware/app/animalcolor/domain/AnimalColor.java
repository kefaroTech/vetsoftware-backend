package com.vetsoftware.app.animalcolor.domain;

import java.time.LocalDateTime;

public class AnimalColor {
    private Long id;
    private String name;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public AnimalColor(Long id, String name, LocalDateTime createdDate, boolean enabled) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        this.id = id;
        this.name = name;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static AnimalColor create(String name) {
        return new AnimalColor(null, name, LocalDateTime.now(), true);
    }

    public void update(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
