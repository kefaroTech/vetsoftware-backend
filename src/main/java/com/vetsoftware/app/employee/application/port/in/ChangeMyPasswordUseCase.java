package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.command.ChangeMyPasswordCommand;
import org.springframework.security.access.prepost.PreAuthorize;

/** Cambia la contraseña del propio empleado autenticado y limpia {@code mustChangePassword}. */
public interface ChangeMyPasswordUseCase {
    @PreAuthorize("isAuthenticated()")
    void execute(ChangeMyPasswordCommand command);
}
