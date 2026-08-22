package com.vetsoftware.app.rolepermission.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SyncRolePermissionsRequest(
        @NotNull(message = "La lista de permisos del rol es obligatoria.") List<Long> permissionIds) {
}
