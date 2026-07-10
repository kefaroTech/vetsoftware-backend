package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.command.ChangeMyPasswordCommand;
import org.springframework.security.access.prepost.PreAuthorize;

/** Cambia la contraseña del propio empleado autenticado y limpia {@code mustChangePassword}. */
public interface ChangeMyPasswordUseCase {
    /**
     * @return {@code true} si era un cambio forzado de primer login (mustChangePassword estaba en true), es
     * decir, el empleado acaba de <b>aceptar su invitación</b>; {@code false} si era un cambio voluntario.
     */
    @PreAuthorize("isAuthenticated()")
    boolean execute(ChangeMyPasswordCommand command);
}
