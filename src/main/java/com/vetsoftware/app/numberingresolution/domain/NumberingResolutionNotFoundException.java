package com.vetsoftware.app.numberingresolution.domain;

public class NumberingResolutionNotFoundException extends RuntimeException {
    public NumberingResolutionNotFoundException(Long id) {
        super("Numbering resolution not found: " + id);
    }
}
