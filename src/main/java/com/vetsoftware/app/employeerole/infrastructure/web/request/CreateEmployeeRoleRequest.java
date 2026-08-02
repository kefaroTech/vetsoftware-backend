package com.vetsoftware.app.employeerole.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record CreateEmployeeRoleRequest(@NotNull Long employeeId, @NotNull Long roleId) {}
