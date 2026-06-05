package com.vetsoftware.app.productchargeopenaccount.domain;

public record AnimalRef(Long id, String name, String code) {
    public AnimalRef {
        if (id == null) throw new IllegalArgumentException("animal id is required");
    }
}
