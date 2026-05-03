package com.vetsoftware.app.auth.application.dto;

import java.util.Set;

public enum SystemContext implements AuthContext {
    INSTANCE;

    @Override
    public Set<String> permissions() {
        return Set.of();
    }
}
