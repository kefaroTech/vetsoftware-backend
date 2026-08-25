package com.vetsoftware.app.platformaccess.domain;

/**
 * Familia de token de invitación: no existe, caducó o ya se consumió. Mismo
 * criterio que {@link InvalidApprovalTokenException} —un solo {@code code}, un
 * solo {@code detail} fijo— y por el mismo motivo: distinguir los tres estados
 * convierte el endpoint en un oráculo de qué invitaciones existieron.
 *
 * <p>
 * Cubre además un cuarto caso que no es de token y que aun así <b>debe</b> ser
 * indistinguible: ya existe un superadministrador con el correo de la
 * solicitud. Responder otra cosa ahí sería un reseteo de contraseña de
 * superadministrador desde un endpoint público.
 */
public class InvalidInvitationTokenException extends RuntimeException {

    public InvalidInvitationTokenException(String message) {
        super(message);
    }
}
