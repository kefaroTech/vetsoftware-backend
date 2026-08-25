package com.vetsoftware.app.platformaccess.application.port.in;

import com.vetsoftware.app.platformaccess.application.dto.PlatformAccessRequestDto;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Resuelve el enlace del aprobador para pintar la solicitud. No consume nada y
 * no cuenta intentos: es una lectura.
 *
 * <p>
 * Los cuatro estados muertos —no existe, caducado, ya decidido y bloqueado—
 * salen todos como la misma excepcion y el mismo codigo. La pantalla de
 * aprobacion no distingue entre ellos, y el backend no debe darle con que.
 */
@NoAuthorizationRequired(reason = "Flujo previo a tener token: la ruta es publica en PublicRoutes y la autorizacion es la posesion del token de un solo uso que trae la propia peticion, mas el codigo de 6 digitos cuando aplica.")
public interface ValidatePlatformAccessTokenUseCase {
    PlatformAccessRequestDto execute(String rawToken);
}
