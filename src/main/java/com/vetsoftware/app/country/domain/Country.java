package com.vetsoftware.app.country.domain;

import java.time.LocalDateTime;

public class Country {
    private Long id;
    private String name;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public Country(Long id, String name, LocalDateTime createdDate, boolean enabled) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        this.id = id;
        this.name = name;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static Country create(String name) {
        return new Country(null, name, LocalDateTime.now(), true);
    }

    public void update(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        this.name = name;
    }

    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
