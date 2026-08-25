package com.vetsoftware.app.platformaccess.application.port.in;

import com.vetsoftware.app.platformaccess.application.command.ResolvePlatformAccessCommand;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Rechaza la solicitud. Exige el mismo token y el mismo codigo que aprobar: no
 * hay un codigo por decision. Rechazar es terminal —no emite invitacion— y el
 * solicitante recibe un aviso sin explicacion del motivo.
 */
@NoAuthorizationRequired(reason = "Flujo previo a tener token: la ruta es publica en PublicRoutes y la autorizacion es la posesion del token de un solo uso que trae la propia peticion, mas el codigo de 6 digitos cuando aplica.")
public interface RejectPlatformAccessRequestUseCase {
    void execute(ResolvePlatformAccessCommand command);
}
