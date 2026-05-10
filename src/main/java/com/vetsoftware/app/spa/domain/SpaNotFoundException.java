package com.vetsoftware.app.spa.domain;

public class SpaNotFoundException extends RuntimeException {
    public SpaNotFoundException(Long id) {
        super("Spa not found: " + id);
    }
}
