package com.vetsoftware.app.systemuserpermission.domain;

public record SystemPermissionRef(Long id, String name, String code) {
    public SystemPermissionRef {
        if (id == null)
            throw new IllegalArgumentException("system permission id is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("system permission name is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("system permission code is required");
    }
}
