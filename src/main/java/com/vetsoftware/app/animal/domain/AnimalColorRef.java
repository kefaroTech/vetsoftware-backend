package com.vetsoftware.app.animal.domain;

public record AnimalColorRef(Long id, String name) {
    public AnimalColorRef {
        if (id == null) throw new IllegalArgumentException("animalColor id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("animalColor name is required");
    }
}
