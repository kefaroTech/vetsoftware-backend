package com.vetsoftware.app.systempermission.domain;

public class SystemPermissionNotFoundException extends RuntimeException {
    public SystemPermissionNotFoundException(Long id) {
        super("SystemPermission not found: " + id);
    }
}
