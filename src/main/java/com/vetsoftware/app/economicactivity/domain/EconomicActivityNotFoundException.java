package com.vetsoftware.app.economicactivity.domain;

public class EconomicActivityNotFoundException extends RuntimeException {
    public EconomicActivityNotFoundException(Long id) {
        super("EconomicActivity not found: " + id);
    }
}
