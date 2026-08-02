package com.vetsoftware.app.submodule.domain;

public record ModuleRef(Long id, String name, String code) {
    public ModuleRef {
        if (id == null)
            throw new IllegalArgumentException("module id is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("module name is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("module code is required");
    }
}
