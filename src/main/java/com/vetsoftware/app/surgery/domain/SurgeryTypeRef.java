package com.vetsoftware.app.surgery.domain;

public record SurgeryTypeRef(Long id, String name) {
    public SurgeryTypeRef {
        if (id == null) throw new IllegalArgumentException("surgery type id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("surgery type name is required");
    }
}
