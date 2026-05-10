package com.vetsoftware.app.surgery.domain;

public class SurgeryNotFoundException extends RuntimeException {
    public SurgeryNotFoundException(Long id) {
        super("Surgery not found: " + id);
    }
}
