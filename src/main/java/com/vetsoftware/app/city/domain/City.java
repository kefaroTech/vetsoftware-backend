package com.vetsoftware.app.city.domain;

import java.time.LocalDateTime;

public class City {
    private Long id;
    private String name;
    private StateRef state;
    private final LocalDateTime createdDate;

    public City(Long id, String name, StateRef state, LocalDateTime createdDate) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (state == null) throw new IllegalArgumentException("state is required");
        this.id = id;
        this.name = name;
        this.state = state;
        this.createdDate = createdDate;
    }

    public static City create(String name, StateRef state) {
        return new City(null, name, state, LocalDateTime.now());
    }

    public void update(String name, StateRef state) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (state == null) throw new IllegalArgumentException("state is required");
        this.name = name;
        this.state = state;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public StateRef getState() { return state; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
