package com.vetsoftware.app.passwordreset.application.port.in;

import com.vetsoftware.app.passwordreset.application.command.ResetPasswordCommand;

/**
 * Confirma el restablecimiento: consume el token y cambia la contraseña del empleado. Sin @PreAuthorize:
 * la autorización es la posesión del token de un solo uso.
 */
public interface ResetPasswordUseCase {
    void execute(ResetPasswordCommand command);
}
