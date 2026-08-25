package com.vetsoftware.app.platformaccess.application.port.in;

import com.vetsoftware.app.platformaccess.application.command.AcceptPlatformInvitationCommand;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Consume la invitacion y crea el superadministrador.
 *
 * <p>
 * <b>Responde 204 sin cuerpo y no emite JWT, refresh ni cookie.</b> No hay
 * autologin: el flujo termina en la pantalla de exito y el usuario entra por el
 * login normal. Emitir sesion aqui convertiria la posesion del token en una
 * sesion viva de superadministrador sin pasar por el login.
 *
 * <p>
 * Si ya existe una cuenta con el correo de la solicitud, <b>no</b> se le cambia
 * la contrasena: sale el mismo error indistinguible que un token muerto.
 * Actualizarla seria un reseteo de contrasena de superadministrador desde un
 * endpoint publico.
 */
@NoAuthorizationRequired(reason = "Flujo previo a tener token: la ruta es publica en PublicRoutes y la autorizacion es la posesion del token de un solo uso que trae la propia peticion, mas el codigo de 6 digitos cuando aplica.")
public interface AcceptPlatformInvitationUseCase {
    void execute(AcceptPlatformInvitationCommand command);
}
