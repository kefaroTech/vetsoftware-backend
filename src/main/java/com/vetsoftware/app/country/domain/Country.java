package com.vetsoftware.app.country.domain;

import java.time.LocalDateTime;

public class Country {
    private Long id;
    private String name;
    private final LocalDateTime createdDate;

    public Country(Long id, String name, LocalDateTime createdDate) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        this.id = id;
        this.name = name;
        this.createdDate = createdDate;
    }

    public static Country create(String name) {
        return new Country(null, name, LocalDateTime.now());
    }

    public void update(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
