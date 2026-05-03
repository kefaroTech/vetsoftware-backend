package com.vetsoftware.app.consultationtype.domain;

public class ConsultationTypeNotFoundException extends RuntimeException {
    public ConsultationTypeNotFoundException(Long id) {
        super("ConsultationType not found: " + id);
    }
}
