package com.vetsoftware.app.rolepermission.testsupport;

import com.vetsoftware.app.rolepermission.domain.PermissionRef;
import com.vetsoftware.app.rolepermission.domain.RolePermission;
import com.vetsoftware.app.rolepermission.domain.RoleRef;
import java.time.LocalDateTime;

/**
 * Fixtures de la feature. Valores validos por defecto, una variante por metodo.
 */
public final class RolePermissionMother {

    public static final Long COMPANY_ID = 9L;
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    public static final RoleRef VETERINARIO = new RoleRef(3L, "Veterinario", "VET");
    public static final RoleRef RECEPCION = new RoleRef(4L, "Recepcion", "REC");
    public static final PermissionRef VER_ANIMALES = new PermissionRef(7L, "Ver animales",
            "ANIMAL_READ");
    public static final PermissionRef CREAR_ANIMALES = new PermissionRef(8L, "Crear animales",
            "ANIMAL_CREATE");

    private RolePermissionMother() {
    }

    /** Asignacion activa con id ya persistido. */
    public static RolePermission activa() {
        return new RolePermission(1L, VETERINARIO, VER_ANIMALES, CREADO, true);
    }

    /** Asignacion desactivada, la que el alta reactiva en vez de duplicar. */
    public static RolePermission desactivada() {
        return new RolePermission(1L, VETERINARIO, VER_ANIMALES, CREADO, false);
    }

    /** Asignacion con el id y el permiso indicados, para listados. */
    public static RolePermission conId(Long id, PermissionRef permission) {
        return new RolePermission(id, VETERINARIO, permission, CREADO, true);
    }
}
