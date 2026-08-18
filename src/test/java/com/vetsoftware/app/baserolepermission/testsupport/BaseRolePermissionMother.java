package com.vetsoftware.app.baserolepermission.testsupport;

import com.vetsoftware.app.baserolepermission.application.command.CreateBaseRolePermissionCommand;
import com.vetsoftware.app.baserolepermission.application.command.UpdateBaseRolePermissionCommand;
import com.vetsoftware.app.baserolepermission.domain.BasePermissionRef;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermission;
import com.vetsoftware.app.baserolepermission.domain.BaseRoleRef;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo baserolepermission.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code BaseRolePermission.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 */
public final class BaseRolePermissionMother {

    public static final Long BASE_ROLE_PERMISSION_ID = 100L;

    public static final BaseRoleRef VETERINARIO = new BaseRoleRef(1L, "Veterinario", "VET");
    public static final BaseRoleRef ADMINISTRADOR = new BaseRoleRef(2L, "Administrador", "ADMIN");

    public static final BasePermissionRef CREAR_CONSULTA = new BasePermissionRef(10L,
            "Crear consulta", "CONSULTA_CREATE");
    public static final BasePermissionRef EDITAR_CONSULTA = new BasePermissionRef(11L,
            "Editar consulta", "CONSULTA_UPDATE");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private BaseRolePermissionMother() {
    }

    /**
     * Vinculo habilitado entre Veterinario y Crear consulta. El caso por defecto.
     */
    public static BaseRolePermission vinculo() {
        return vinculo(BASE_ROLE_PERMISSION_ID);
    }

    public static BaseRolePermission vinculo(Long id) {
        return new BaseRolePermission(id, VETERINARIO, CREAR_CONSULTA, CREADO, true);
    }

    public static BaseRolePermission deshabilitado() {
        return new BaseRolePermission(BASE_ROLE_PERMISSION_ID, VETERINARIO, CREAR_CONSULTA, CREADO,
                false);
    }

    /** Comando de creacion coherente con las refs de arriba. */
    public static CreateBaseRolePermissionCommand comandoCrear() {
        return new CreateBaseRolePermissionCommand(VETERINARIO.id(), CREAR_CONSULTA.id());
    }

    /** Comando de actualizacion que cambia rol y permiso. */
    public static UpdateBaseRolePermissionCommand comandoActualizar() {
        return new UpdateBaseRolePermissionCommand(BASE_ROLE_PERMISSION_ID, ADMINISTRADOR.id(),
                EDITAR_CONSULTA.id());
    }
}
