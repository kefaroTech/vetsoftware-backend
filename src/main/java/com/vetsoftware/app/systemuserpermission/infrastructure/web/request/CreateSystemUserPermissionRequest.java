package com.vetsoftware.app.systemuserpermission.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record CreateSystemUserPermissionRequest(
    @NotNull Long systemUserId, @NotNull Long systemPermissionId) {}
