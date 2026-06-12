package com.vetsoftware.app.companytaxprofile.domain;

public record EconomicActivityRef(Long id, String code, String name) {
    public EconomicActivityRef {
        if (id == null) throw new IllegalArgumentException("economic activity id is required");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("economic activity code is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("economic activity name is required");
    }
}
