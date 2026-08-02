package com.vetsoftware.app.systemuserpermission.domain;

public record SystemUserRef(Long id, String code) {
    public SystemUserRef {
        if (id == null)
            throw new IllegalArgumentException("system user id is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("system user code is required");
    }
}
