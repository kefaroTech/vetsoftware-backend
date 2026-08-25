package com.vetsoftware.app.platformaccess.application.port.in;

import com.vetsoftware.app.platformaccess.application.dto.PlatformInvitationDto;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Resuelve el enlace de la invitacion para pintar la pantalla de crear
 * contrasena. Devuelve el correo de la solicitud, que es el unico dato que esa
 * pantalla necesita. No consume la invitacion.
 */
@NoAuthorizationRequired(reason = "Flujo previo a tener token: la ruta es publica en PublicRoutes y la autorizacion es la posesion del token de un solo uso que trae la propia peticion, mas el codigo de 6 digitos cuando aplica.")
public interface ValidatePlatformInvitationTokenUseCase {
    PlatformInvitationDto execute(String rawToken);
}
