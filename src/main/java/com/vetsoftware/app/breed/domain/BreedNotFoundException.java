package com.vetsoftware.app.breed.domain;

public class BreedNotFoundException extends RuntimeException {
    public BreedNotFoundException(Long id) {
        super("Breed not found: " + id);
    }
}
