package com.vetsoftware.app.module.domain;

public class ModuleNotFoundException extends RuntimeException {
    public ModuleNotFoundException(Long id) {
        super("Module not found: " + id);
    }
}
