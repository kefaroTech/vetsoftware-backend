package com.vetsoftware.app.surgerytype.domain;

public class SurgeryTypeNotFoundException extends RuntimeException {
    public SurgeryTypeNotFoundException(Long id) {
        super("SurgeryType not found: " + id);
    }
}
