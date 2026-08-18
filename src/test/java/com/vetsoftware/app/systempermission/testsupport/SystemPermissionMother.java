package com.vetsoftware.app.systempermission.testsupport;

import com.vetsoftware.app.systempermission.domain.SystemPermission;
import java.time.LocalDateTime;

public final class SystemPermissionMother {

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private SystemPermissionMother() {
    }

    public static SystemPermission permisoValido() {
        return new SystemPermission(1L, "Administrar usuarios", "admin.users", CREADO, true);
    }
}
