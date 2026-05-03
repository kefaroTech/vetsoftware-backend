package com.vetsoftware.app.prescription.domain;

public record AnimalRef(Long id, String name, String code) {
    public AnimalRef {
        if (id == null) throw new IllegalArgumentException("animal id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("animal name is required");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("animal code is required");
    }
}
