package com.vetsoftware.app.animalcolor.domain;

public class AnimalColorNotFoundException extends RuntimeException {
    public AnimalColorNotFoundException(Long id) {
        super("AnimalColor not found: " + id);
    }
}
