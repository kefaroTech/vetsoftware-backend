package com.vetsoftware.app.module.testsupport;

import com.vetsoftware.app.module.domain.Module;
import java.time.LocalDateTime;

public final class ModuleMother {

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private ModuleMother() {
    }

    public static Module moduloValido() {
        return new Module(1L, "Inventario", "INV", CREADO, null, true);
    }
}
