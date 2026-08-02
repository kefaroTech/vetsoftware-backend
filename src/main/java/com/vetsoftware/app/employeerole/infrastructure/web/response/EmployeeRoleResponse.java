package com.vetsoftware.app.employeerole.infrastructure.web.response;

import java.time.LocalDateTime;

public record EmployeeRoleResponse(
    Long id,
    EmployeeSummary employee,
    RoleSummary role,
    LocalDateTime createdDate,
    boolean enabled) {}
