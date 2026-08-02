package com.vetsoftware.app.coderecovery.application.port.in;

import com.vetsoftware.app.coderecovery.application.command.RecoverEmployeeCodeCommand;

/**
 * Envía por correo el/los código(s) de acceso asociados a un email.
 * Sin @PreAuthorize: flujo público. Anti-enumeración: siempre completa sin
 * error, encuentre o no cuentas.
 */
public interface RecoverEmployeeCodeUseCase {
    void execute(RecoverEmployeeCodeCommand command);
}
