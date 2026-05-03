package com.vetsoftware.app.auth.application.dto;

import java.util.Set;

public record EmployeeContext(Long employeeId, Long companyId, Set<String> permissions)
    implements AuthContext {
}
