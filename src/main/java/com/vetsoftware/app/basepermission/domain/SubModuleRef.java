package com.vetsoftware.app.basepermission.domain;

public record SubModuleRef(Long id, String name, String code) {
    public SubModuleRef {
        if (id == null)
            throw new IllegalArgumentException("subModule id is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("subModule name is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("subModule code is required");
    }
}
