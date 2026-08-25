package com.vetsoftware.app.platformaccess.application.port.in;

import com.vetsoftware.app.platformaccess.application.command.ResolvePlatformAccessCommand;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Aprueba la solicitud y emite la invitacion.
 *
 * <p>
 * <b>El codigo de 6 digitos NO es un segundo factor.</b> Viaja en el mismo
 * correo que el enlace, por decision humana explicita tomada con el riesgo
 * delante: quien tenga acceso al buzon del aprobador tiene los dos elementos.
 * Su funcion es confirmar la intencion —evitar la aprobacion por un clic
 * accidental sobre el enlace— y separar "alguien reenvio el correo entero" de
 * "el aprobador decidio". El contador de 5 intentos sigue siendo necesario:
 * protege el caso de quien obtuvo o adivino el token pero no el correo. Un
 * factor fuera de banda real (secreto pre-compartido o TOTP) queda como mejora
 * posterior y NO forma parte de este flujo.
 *
 * <p>
 * Aprobar crea, aguas abajo, una cuenta con control total de la plataforma:
 * {@code AuthFilter} concede {@code ROLE_SYSTEM} a todo contexto de usuario de
 * sistema sin mirar permisos. Por eso este caso de uso no acepta permisos,
 * roles ni banderas del cliente, no llama a ningun caso de uso de permisos y no
 * copia los de ninguna otra cuenta.
 */
@NoAuthorizationRequired(reason = "Flujo previo a tener token: la ruta es publica en PublicRoutes y la autorizacion es la posesion del token de un solo uso que trae la propia peticion, mas el codigo de 6 digitos cuando aplica.")
public interface ApprovePlatformAccessRequestUseCase {
    void execute(ResolvePlatformAccessCommand command);
}
