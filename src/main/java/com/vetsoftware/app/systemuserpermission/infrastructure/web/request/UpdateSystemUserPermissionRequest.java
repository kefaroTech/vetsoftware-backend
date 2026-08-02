package com.vetsoftware.app.systemuserpermission.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record UpdateSystemUserPermissionRequest(@NotNull Long systemUserId,
        @NotNull Long systemPermissionId) {
}
