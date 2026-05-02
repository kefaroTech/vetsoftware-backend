package com.vetsoftware.app.state.domain;

import java.time.LocalDateTime;

public class State {
    private Long id;
    private String name;
    private CountryRef country;
    private final LocalDateTime createdDate;

    public State(Long id, String name, CountryRef country, LocalDateTime createdDate) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (country == null) throw new IllegalArgumentException("country is required");
        this.id = id;
        this.name = name;
        this.country = country;
        this.createdDate = createdDate;
    }

    public static State create(String name, CountryRef country) {
        return new State(null, name, country, LocalDateTime.now());
    }

    public void update(String name, CountryRef country) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (country == null) throw new IllegalArgumentException("country is required");
        this.name = name;
        this.country = country;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public CountryRef getCountry() { return country; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
