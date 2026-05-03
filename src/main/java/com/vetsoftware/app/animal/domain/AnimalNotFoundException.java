package com.vetsoftware.app.animal.domain;

public class AnimalNotFoundException extends RuntimeException {
    public AnimalNotFoundException(Long id) {
        super("Animal not found: " + id);
    }
}
