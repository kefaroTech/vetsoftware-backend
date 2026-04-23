package com.vetsoftware.app.submodule.domain;

public class SubModuleNotFoundException extends RuntimeException {
    public SubModuleNotFoundException(Long id) {
        super("SubModule not found: " + id);
    }
}
