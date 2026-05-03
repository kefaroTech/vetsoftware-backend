package com.vetsoftware.app.auth.application.dto;

import com.vetsoftware.app.auth.application.exception.UnauthorizedException;
import java.util.Arrays;
import java.util.Set;

public record AuthContext(Long employeeId, Long companyId, Set<String> permissions) {
    public void requireAnyPermission(String... required) {
        boolean hasAny = Arrays.stream(required).anyMatch(permissions::contains);
        if (!hasAny)
            throw new UnauthorizedException(String.join(" or ", required));
    }
}
