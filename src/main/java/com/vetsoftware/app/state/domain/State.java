package com.vetsoftware.app.state.domain;

import java.time.LocalDateTime;

public class State {
    private Long id;
    private String name;
    private CountryRef country;
    private String daneCode;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public State(Long id, String name, CountryRef country, String daneCode, LocalDateTime createdDate, boolean enabled) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (country == null) throw new IllegalArgumentException("country is required");
        this.id = id;
        this.name = name;
        this.country = country;
        this.daneCode = daneCode;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static State create(String name, CountryRef country, String daneCode) {
        return new State(null, name, country, daneCode, LocalDateTime.now(), true);
    }

    public void update(String name, CountryRef country, String daneCode) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (country == null) throw new IllegalArgumentException("country is required");
        this.name = name;
        this.country = country;
        this.daneCode = daneCode;
    }

    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public CountryRef getCountry() { return country; }
    public String getDaneCode() { return daneCode; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
