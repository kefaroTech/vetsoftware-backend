package com.vetsoftware.app.state.domain;

public class StateNotFoundException extends RuntimeException {
    public StateNotFoundException(Long id) {
        super("State not found: " + id);
    }
}
