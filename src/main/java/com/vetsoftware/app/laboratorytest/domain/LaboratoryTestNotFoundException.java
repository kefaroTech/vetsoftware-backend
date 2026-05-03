package com.vetsoftware.app.laboratorytest.domain;

public class LaboratoryTestNotFoundException extends RuntimeException {
    public LaboratoryTestNotFoundException(Long id) {
        super("LaboratoryTest not found: " + id);
    }
}
