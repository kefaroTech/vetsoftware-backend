package com.vetsoftware.app.platformaccess.application.port.out;

import java.time.LocalDateTime;

/**
 * Los cuatro correos del flujo. Todos se disparan en {@code afterCommit} y
 * ninguno lanza: el envio es {@code @Async} y best-effort, asi que un correo
 * que no sale <b>se pierde sin reintento</b>. Eso esta asumido en el diseno —la
 * solicitud se puede repetir— y es lo que obliga a que el fallo del correo de
 * invitacion deje un evento de auditoria en ERROR.
 *
 * <p>
 * {@code requestId} viaja en todas las firmas a proposito: el adaptador corre
 * en el pool de correo, al otro lado de un salto de hilo, y necesita poder
 * decir de que solicitud habla sin depender de que el MDC haya cruzado.
 */
public interface PlatformAccessEmailSender {

    /**
     * Aviso al aprobador, con el enlace y el codigo. {@code fullName} y
     * {@code reason} son texto libre de un desconocido y se escapan antes de entrar
     * en las variables de la plantilla.
     */
    void sendAccessRequested(AccessRequestedNotification notification);

    /** Invitacion al solicitante tras aprobar. Su desenlace SI se vigila. */
    void sendInvitation(Long requestId, String toEmail, String fullName, String rawInvitationToken);

    /** Aviso de rechazo al solicitante. Sin motivo: el rechazo no se justifica. */
    void sendRejection(Long requestId, String toEmail, String fullName);

    /**
     * Bienvenida tras aceptar, con el codigo de usuario.
     *
     * <p>
     * <b>No es cortesia.</b> El login de las cuentas de sistema es por
     * {@code code}, no por correo, y el flujo solo pide contrasena: sin este correo
     * la cuenta queda creada y su dueno sin saber con que usuario entrar.
     */
    void sendWelcome(Long requestId, String toEmail, String fullName, String systemUserCode);

    /**
     * Payload del aviso al aprobador, resuelto <b>dentro</b> de la transaccion. Se
     * pasa entero al callback para que este no tenga que volver a leer nada despues
     * del commit.
     */
    record AccessRequestedNotification(Long requestId, String fullName, String requesterEmail,
            String reason, LocalDateTime requestedAt, String rawApprovalToken,
            String verificationCode) {
    }
}
