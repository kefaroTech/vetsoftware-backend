package com.vetsoftware.app.city.domain;

public class CityNotFoundException extends RuntimeException {
    public CityNotFoundException(Long id) {
        super("City not found: " + id);
    }
}
