package com.vetsoftware.app.systemuserpermission.domain;

public class SystemUserPermissionNotFoundException extends RuntimeException {
    public SystemUserPermissionNotFoundException(Long id) {
        super("SystemUserPermission not found: " + id);
    }
}
