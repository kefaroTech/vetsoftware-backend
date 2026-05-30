package com.vetsoftware.app.laboratorytestfile.domain;

public class LaboratoryTestFileNotFoundException extends RuntimeException {
    public LaboratoryTestFileNotFoundException(Long id) {
        super("LaboratoryTestFile not found: " + id);
    }
}
