package com.vetsoftware.app.rolepermission.domain;

public class RolePermissionNotFoundException extends RuntimeException {
    public RolePermissionNotFoundException(Long id) {
        super("RolePermission not found: " + id);
    }
}
