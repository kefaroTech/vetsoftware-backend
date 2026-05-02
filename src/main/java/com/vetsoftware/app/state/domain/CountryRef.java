package com.vetsoftware.app.state.domain;

public record CountryRef(Long id, String name) {
    public CountryRef {
        if (id == null) throw new IllegalArgumentException("country id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("country name is required");
    }
}
