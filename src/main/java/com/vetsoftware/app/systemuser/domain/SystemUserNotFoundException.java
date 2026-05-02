package com.vetsoftware.app.systemuser.domain;

public class SystemUserNotFoundException extends RuntimeException {
    public SystemUserNotFoundException(Long id) {
        super("SystemUser not found: " + id);
    }
}
