package com.vetsoftware.app.baserole.domain;

public class BaseRoleNotFoundException extends RuntimeException {
    public BaseRoleNotFoundException(Long id) {
        super("BaseRole not found: " + id);
    }
}
