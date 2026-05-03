package com.vetsoftware.app.auth.application.dto;

public enum SystemContext implements AuthContext {
    INSTANCE;

    @Override
    public void requireAnyPermission(String... required) {
        // Trusted internal caller: no permission check.
    }
}
