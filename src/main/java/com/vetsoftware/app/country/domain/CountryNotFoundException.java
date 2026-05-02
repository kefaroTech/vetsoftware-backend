package com.vetsoftware.app.country.domain;

public class CountryNotFoundException extends RuntimeException {
    public CountryNotFoundException(Long id) {
        super("Country not found: " + id);
    }
}
