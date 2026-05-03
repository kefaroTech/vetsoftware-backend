package com.vetsoftware.app.laboratorytest.domain;

public record TestTypeRef(Long id, String name) {
    public TestTypeRef {
        if (id == null) throw new IllegalArgumentException("test type id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("test type name is required");
    }
}
