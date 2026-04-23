package com.vetsoftware.app.auth.application.port.out;

import java.util.Set;

public interface PermissionResolver {
    Set<String> resolveFor(Long employeeId);
}
