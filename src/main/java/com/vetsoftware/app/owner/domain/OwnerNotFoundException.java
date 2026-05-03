package com.vetsoftware.app.owner.domain;

public class OwnerNotFoundException extends RuntimeException {
    public OwnerNotFoundException(Long id) {
        super("Owner not found: " + id);
    }
}
