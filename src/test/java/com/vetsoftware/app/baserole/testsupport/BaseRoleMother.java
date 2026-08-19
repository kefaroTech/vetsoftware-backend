package com.vetsoftware.app.baserole.testsupport;

import com.vetsoftware.app.baserole.domain.BaseRole;
import java.time.LocalDateTime;

public final class BaseRoleMother {

    public static final Long BASE_ROLE_ID = 1L;
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private BaseRoleMother() {
    }

    public static BaseRole veterinario() {
        return new BaseRole(BASE_ROLE_ID, "Veterinario", "VET", false, CREADO, null, true);
    }

    public static BaseRole administrador() {
        return new BaseRole(2L, "Administrador", "ADMIN", true, CREADO, null, true);
    }
}
