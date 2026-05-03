package com.vetsoftware.app.auth.application.dto;

import com.vetsoftware.app.auth.application.exception.UnauthorizedException;
import java.util.Arrays;
import java.util.Set;

public sealed interface AuthContext
    permits EmployeeContext, SystemUserContext, SystemContext {

    void requireAnyPermission(String... required);

    static void check(Set<String> held, String... required) {
        boolean hasAny = Arrays.stream(required).anyMatch(held::contains);
        if (!hasAny)
            throw new UnauthorizedException(String.join(" or ", required));
    }
}
