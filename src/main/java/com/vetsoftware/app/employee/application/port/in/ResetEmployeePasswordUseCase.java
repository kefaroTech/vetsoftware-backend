package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.command.ResetEmployeePasswordCommand;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Aplica una contraseña nueva al empleado (hashea, limpia mustChangePassword e
 * invalida sesiones vivas). Sin @PreAuthorize: se invoca desde el flujo público
 * de restablecimiento, donde la autorización es la posesión del token de un
 * solo uso (ver passwordreset.ResetPasswordService).
 */
@NoAuthorizationRequired(reason = "Puerto interno de orquestación: ningún controller lo expone. Corre dentro de la transacción del caso de uso que lo invoca, que ya validó permiso y ownership.")
public interface ResetEmployeePasswordUseCase {
    void execute(ResetEmployeePasswordCommand command);
}
