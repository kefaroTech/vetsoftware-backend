package com.vetsoftware.app.baserolepermission.domain;

public record BaseRoleRef(Long id, String name, String code) {
    public BaseRoleRef {
        if (id == null) throw new IllegalArgumentException("baseRole id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("baseRole name is required");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("baseRole code is required");
    }
}
