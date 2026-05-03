package com.vetsoftware.app.vaccination.domain;

public class VaccinationNotFoundException extends RuntimeException {
    public VaccinationNotFoundException(Long id) {
        super("Vaccination not found: " + id);
    }
}
