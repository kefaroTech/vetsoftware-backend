package com.vetsoftware.app.baserolepermission.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record CreateBaseRolePermissionRequest(
        @NotNull(message = "Debes seleccionar el rol base.") Long baseRoleId,
        @NotNull(message = "Debes seleccionar el permiso base.") Long basePermissionId) {
}
