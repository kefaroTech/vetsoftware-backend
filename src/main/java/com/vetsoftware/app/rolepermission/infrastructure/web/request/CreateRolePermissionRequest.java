package com.vetsoftware.app.rolepermission.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record CreateRolePermissionRequest(@NotNull Long roleId, @NotNull Long permissionId) {}
