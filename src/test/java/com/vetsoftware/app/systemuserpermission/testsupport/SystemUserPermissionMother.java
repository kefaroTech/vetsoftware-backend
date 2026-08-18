package com.vetsoftware.app.systemuserpermission.testsupport;

import com.vetsoftware.app.systemuserpermission.application.command.CreateSystemUserPermissionCommand;
import com.vetsoftware.app.systemuserpermission.application.command.UpdateSystemUserPermissionCommand;
import com.vetsoftware.app.systemuserpermission.domain.SystemPermissionRef;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermission;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserRef;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo systemuserpermission.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code SystemUserPermission.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 */
public final class SystemUserPermissionMother {

    public static final Long ID = 100L;

    public static final SystemUserRef USUARIO = new SystemUserRef(5L, "admin-api");
    public static final SystemPermissionRef PERMISO = new SystemPermissionRef(8L,
            "Gestionar Reportes", "reports.manage");

    public static final SystemUserRef OTRO_USUARIO = new SystemUserRef(6L, "soporte-api");
    public static final SystemPermissionRef OTRO_PERMISO = new SystemPermissionRef(9L,
            "Gestionar Facturas", "billing.manage");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private SystemUserPermissionMother() {
    }

    /** Asignacion activa, habilitada. El caso por defecto. */
    public static SystemUserPermission asignacionActiva() {
        return asignacionActiva(ID);
    }

    public static SystemUserPermission asignacionActiva(Long id) {
        return new SystemUserPermission(id, USUARIO, PERMISO, CREADO, true);
    }

    public static SystemUserPermission asignacionDeshabilitada() {
        return new SystemUserPermission(ID, USUARIO, PERMISO, CREADO, false);
    }

    public static CreateSystemUserPermissionCommand comandoCrear() {
        return new CreateSystemUserPermissionCommand(USUARIO.id(), PERMISO.id());
    }

    public static UpdateSystemUserPermissionCommand comandoActualizar() {
        return new UpdateSystemUserPermissionCommand(ID, OTRO_USUARIO.id(), OTRO_PERMISO.id());
    }
}
