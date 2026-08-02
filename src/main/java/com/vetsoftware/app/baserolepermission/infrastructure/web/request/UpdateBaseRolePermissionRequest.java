package com.vetsoftware.app.baserolepermission.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record UpdateBaseRolePermissionRequest(
    @NotNull Long baseRoleId, @NotNull Long basePermissionId) {}
