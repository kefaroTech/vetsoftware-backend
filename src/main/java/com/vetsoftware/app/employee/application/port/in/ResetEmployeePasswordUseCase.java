package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.command.ResetEmployeePasswordCommand;

/**
 * Aplica una contraseña nueva al empleado (hashea, limpia mustChangePassword e invalida sesiones
 * vivas). Sin @PreAuthorize: se invoca desde el flujo público de restablecimiento, donde la
 * autorización es la posesión del token de un solo uso (ver passwordreset.ResetPasswordService).
 */
public interface ResetEmployeePasswordUseCase {
  void execute(ResetEmployeePasswordCommand command);
}
