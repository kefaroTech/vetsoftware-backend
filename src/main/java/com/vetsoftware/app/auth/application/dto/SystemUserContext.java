package com.vetsoftware.app.auth.application.dto;

import java.util.Set;

public record SystemUserContext(Long systemUserId, Set<String> permissions)
    implements AuthContext {
}
