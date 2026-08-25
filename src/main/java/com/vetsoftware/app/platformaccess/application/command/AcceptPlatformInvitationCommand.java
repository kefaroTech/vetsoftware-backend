package com.vetsoftware.app.platformaccess.application.command;

/**
 * Token de la invitación más la contraseña elegida. <b>No lleva correo, y no
 * debe llevarlo nunca:</b> el correo de la cuenta que nace sale del token, no
 * del cuerpo. Uno que viajase aquí permitiría a quien posee una invitación
 * legítima elegir la identidad del superadministrador que se va a crear.
 */
public record AcceptPlatformInvitationCommand(String token, String password) {
}
