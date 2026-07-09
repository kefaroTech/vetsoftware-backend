package com.vetsoftware.app.appointment.domain;

public record CompanyRef(Long id) {
    public CompanyRef {
        if (id == null) throw new IllegalArgumentException("company id is required");
    }

    public static CompanyRef of(Long id) {
        return new CompanyRef(id);
    }
}
