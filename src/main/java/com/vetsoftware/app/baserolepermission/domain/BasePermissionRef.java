package com.vetsoftware.app.baserolepermission.domain;

public record BasePermissionRef(Long id, String name, String code) {
    public BasePermissionRef {
        if (id == null)
            throw new IllegalArgumentException("basePermission id is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("basePermission name is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("basePermission code is required");
    }
}
