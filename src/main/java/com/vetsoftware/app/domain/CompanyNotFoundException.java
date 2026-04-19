package com.vetsoftware.app.domain;

public class CompanyNotFoundException extends RuntimeException {
    public CompanyNotFoundException(Long id) {
        super("Company not found: " + id);
    }
}
