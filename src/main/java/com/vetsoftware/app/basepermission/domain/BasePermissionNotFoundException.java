package com.vetsoftware.app.basepermission.domain;

public class BasePermissionNotFoundException extends RuntimeException {
    public BasePermissionNotFoundException(Long id) {
        super("BasePermission not found with id: " + id);
    }
}
