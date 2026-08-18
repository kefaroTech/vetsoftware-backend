package com.vetsoftware.app.systemuser.testsupport;

import com.vetsoftware.app.systemuser.application.command.CreateSystemUserCommand;
import com.vetsoftware.app.systemuser.application.command.UpdateSystemUserCommand;
import com.vetsoftware.app.systemuser.domain.SystemUser;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo systemuser.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code SystemUser.create(...)}: el factory pone {@code LocalDateTime.now()} y
 * haria no deterministas las aserciones sobre {@code createdDate}.
 */
public final class SystemUserMother {

    public static final Long SYSTEM_USER_ID = 100L;
    public static final String CODE = "svc-integracion";
    public static final String HASH_PASSWORD = "hash-almacenado-de-prueba";
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private SystemUserMother() {
    }

    /** Usuario de sistema activo, sin rotaciones de sesion. El caso por defecto. */
    public static SystemUser activo() {
        return activo(SYSTEM_USER_ID);
    }

    public static SystemUser activo(Long id) {
        return new SystemUser(id, CODE, HASH_PASSWORD, CREADO, true, 0L);
    }

    public static SystemUser deshabilitado() {
        return new SystemUser(SYSTEM_USER_ID, CODE, HASH_PASSWORD, CREADO, false, 0L);
    }

    public static SystemUser conAuthVersion(long authVersion) {
        return new SystemUser(SYSTEM_USER_ID, CODE, HASH_PASSWORD, CREADO, true, authVersion);
    }

    /** Comando de creacion coherente con las constantes de arriba. */
    public static CreateSystemUserCommand comandoCrear() {
        return new CreateSystemUserCommand(CODE, "unaContrasenaSegura1");
    }

    public static UpdateSystemUserCommand comandoActualizar() {
        return new UpdateSystemUserCommand(SYSTEM_USER_ID, "svc-actualizado");
    }
}
