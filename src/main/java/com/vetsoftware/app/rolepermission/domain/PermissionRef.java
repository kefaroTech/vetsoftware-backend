package com.vetsoftware.app.rolepermission.domain;

public record PermissionRef(Long id, String name, String code) {
    public PermissionRef {
        if (id == null) throw new IllegalArgumentException("permission id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("permission name is required");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("permission code is required");
    }
}
