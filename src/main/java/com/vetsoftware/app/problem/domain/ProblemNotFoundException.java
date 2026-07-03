package com.vetsoftware.app.problem.domain;

public class ProblemNotFoundException extends RuntimeException {
    public ProblemNotFoundException(Long id) {
        super("Problem not found: " + id);
    }
}
