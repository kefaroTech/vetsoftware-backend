package com.vetsoftware.app.testtype.domain;

public class TestTypeNotFoundException extends RuntimeException {
    public TestTypeNotFoundException(Long id) {
        super("TestType not found: " + id);
    }
}
