package com.vetsoftware.app.platformaccess.application.port.in;

import com.vetsoftware.app.platformaccess.application.command.RequestPlatformAccessCommand;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Recibe una solicitud publica de alta de superadministrador.
 *
 * <p>
 * <b>Responde igual exista o no ya una cuenta con ese correo.</b> Distinguirlo
 * convertiria el formulario en un directorio de superadministradores de la
 * plataforma, que es la lista de objetivos mas valiosa del sistema. Al
 * solicitante tampoco se le manda nada: un correo de "ya tienes cuenta" seria
 * el mismo oraculo por un canal lateral.
 *
 * <p>
 * El unico desenlace distinto es el formulario cerrado, que se decide ANTES de
 * mirar el correo para que la latencia no distinga las dos ramas.
 */
@NoAuthorizationRequired(reason = "Flujo previo a tener token: la ruta es publica en PublicRoutes y la autorizacion es la posesion del token de un solo uso que trae la propia peticion, mas el codigo de 6 digitos cuando aplica.")
public interface RequestPlatformAccessUseCase {
    void execute(RequestPlatformAccessCommand command);
}
