package com.vetsoftware.app.vaccinationtype.domain;

public class VaccinationTypeNotFoundException extends RuntimeException {
    public VaccinationTypeNotFoundException(Long id) {
        super("VaccinationType not found: " + id);
    }
}
