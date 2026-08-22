package com.vetsoftware.app.systemuserpermission.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record CreateSystemUserPermissionRequest(
        @NotNull(message = "Debes seleccionar el usuario de sistema.") Long systemUserId,
        @NotNull(message = "Debes seleccionar el permiso de sistema.") Long systemPermissionId) {
}
