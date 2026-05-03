package com.vetsoftware.app.consultation.domain;

public class ConsultationNotFoundException extends RuntimeException {
    public ConsultationNotFoundException(Long id) {
        super("Consultation not found: " + id);
    }
}
