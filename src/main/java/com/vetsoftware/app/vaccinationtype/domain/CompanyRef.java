package com.vetsoftware.app.vaccinationtype.domain;

public record CompanyRef(Long id, String name, String identifier) {
    public CompanyRef {
        if (id == null)
            throw new IllegalArgumentException("company id is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("company name is required");
        if (identifier == null || identifier.isBlank())
            throw new IllegalArgumentException("company identifier is required");
    }
}
