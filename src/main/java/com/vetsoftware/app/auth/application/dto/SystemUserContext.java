package com.vetsoftware.app.auth.application.dto;

import java.util.Set;

public record SystemUserContext(Long systemUserId, Set<String> permissions)
    implements AuthContext {

    @Override
    public void requireAnyPermission(String... required) {
        AuthContext.check(permissions, required);
    }
}
