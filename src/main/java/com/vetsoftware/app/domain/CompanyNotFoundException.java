package com.vetsoftware.app.domain;

public class CompanyNotFoundException extends RuntimeException {
    public CompanyNotFoundException(String id) {
        super("Company not found: " + id);
    }
}
