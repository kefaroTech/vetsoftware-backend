package com.vetsoftware.app.appointment.domain;

public record OwnerRef(Long id, String name) {
    public OwnerRef {
        if (id == null) throw new IllegalArgumentException("owner id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("owner name is required");
    }
}
