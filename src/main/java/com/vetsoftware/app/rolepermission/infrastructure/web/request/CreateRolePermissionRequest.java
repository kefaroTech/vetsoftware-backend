package com.vetsoftware.app.rolepermission.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record CreateRolePermissionRequest(
        @NotNull(message = "Debes seleccionar el rol.") Long roleId,
        @NotNull(message = "Debes seleccionar el permiso.") Long permissionId) {
}
