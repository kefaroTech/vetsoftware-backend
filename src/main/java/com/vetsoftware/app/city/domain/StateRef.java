package com.vetsoftware.app.city.domain;

public record StateRef(Long id, String name) {
    public StateRef {
        if (id == null)
            throw new IllegalArgumentException("state id is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("state name is required");
    }
}
