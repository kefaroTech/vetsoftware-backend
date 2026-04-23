package com.vetsoftware.app.auth.application.dto;

import com.vetsoftware.app.auth.application.exception.UnauthorizedException;
import java.util.Set;

public record AuthContext(Long employeeId, Set<String> permissions) {
    public void requirePermission(String permission) {
        if (!permissions.contains(permission))
            throw new UnauthorizedException(permission);
    }
}
