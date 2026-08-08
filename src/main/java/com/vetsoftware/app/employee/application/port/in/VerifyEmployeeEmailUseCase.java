package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Marca el correo del empleado como verificado. Sin @PreAuthorize: se invoca
 * desde el flujo publico de verificacion, donde la autorizacion es la posesion
 * del token de un solo uso (ver VerifyEmailService).
 */
@NoAuthorizationRequired(reason = "Puerto interno del flujo público de verificación: ningún controller lo expone y la autorización es la posesión del token de un solo uso que ya validó VerifyEmailService.")
public interface VerifyEmployeeEmailUseCase {
    void execute(Long employeeId);
}
