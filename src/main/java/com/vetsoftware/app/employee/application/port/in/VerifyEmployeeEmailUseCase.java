package com.vetsoftware.app.employee.application.port.in;

/**
 * Marca el correo del empleado como verificado. Sin @PreAuthorize: se invoca
 * desde el flujo publico de verificacion, donde la autorizacion es la posesion
 * del token de un solo uso (ver VerifyEmailService).
 */
public interface VerifyEmployeeEmailUseCase {
    void execute(Long employeeId);
}
