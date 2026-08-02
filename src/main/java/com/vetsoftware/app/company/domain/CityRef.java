package com.vetsoftware.app.company.domain;

public record CityRef(Long id, String name) {
    public CityRef {
        if (id == null)
            throw new IllegalArgumentException("city id is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("city name is required");
    }
}
