package com.vetsoftware.app.dunning.domain;

public class DunningEventNotFoundException extends RuntimeException {
    public DunningEventNotFoundException(Long id) {
        super("DunningEvent not found: " + id);
    }
}
