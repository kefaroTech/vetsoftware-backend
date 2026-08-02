package com.vetsoftware.app.employeerole.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record UpdateEmployeeRoleRequest(@NotNull Long employeeId, @NotNull Long roleId) {}
