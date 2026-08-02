package com.vetsoftware.app.rolepermission.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record UpdateRolePermissionRequest(@NotNull Long roleId, @NotNull Long permissionId) {}
